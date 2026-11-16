package jp.co.frontier.ems.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jp.co.frontier.ems.common.error.ApiException;
import jp.co.frontier.ems.common.error.ErrorCode;
import jp.co.frontier.ems.dto.EmployeeSearchCriteria;
import jp.co.frontier.ems.dto.EmployeeUpsertRequest;
import jp.co.frontier.ems.entity.Employee;
import jp.co.frontier.ems.entity.ImportJob;
import jp.co.frontier.ems.repository.EmployeeRepository;
import jp.co.frontier.ems.repository.ImportJobRepository;
import jp.co.frontier.ems.security.AuthenticatedEmployee;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 社員情報のCSV一括取込・出力（API 10.1〜10.3）。ADMIN 専用。
 *
 * <p>取込は仕様どおり非同期（202 を返してから処理する）。
 */
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeCsvService {

    /** CSVのヘッダー行。この順序・名称で1行目を要求する */
    static final List<String> CSV_HEADER = List.of(
            "employee_code", "name", "name_kana", "email", "tel",
            "hire_date", "department_id", "position_id", "employment_type_id", "seat_id");

    /** 取込の行数上限（仕様書 10.1） */
    static final int MAX_ROWS = 10_000;

    private static final Logger log = LoggerFactory.getLogger(EmployeeCsvService.class);
    private static final DateTimeFormatter JOB_ID_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ImportJobRepository importJobRepository;
    private final EmployeeAdminService employeeAdminService;
    private final EmployeeRepository employeeRepository;
    private final EmployeeCsvImportWorker importWorker;

    /**
     * 取込の受付（API 10.1）。
     *
     * <p>ファイルの形式を確認してジョブを作り、202 を返す。実際の取り込みは
     * {@link EmployeeCsvAsyncRunner} が別スレッドで進める。
     *
     * <p>ファイルの内容は受付時にすべて読み切っている。非同期側で
     * {@code MultipartFile} を触ると、リクエストが終わった時点で一時ファイルが
     * 消えていて読めなくなるため。
     */
    @Transactional
    public ImportJob accept(AuthenticatedEmployee executor, MultipartFile file) {
        List<String[]> rows = 読み込む(file);

        if (rows.size() > MAX_ROWS) {
            throw ApiException.validation(
                    "取り込める行数は%,d行までです（%,d行ありました）"
                            .formatted(MAX_ROWS, rows.size()), null);
        }

        Employee 実行者 = employeeRepository.findById(executor.employeeId())
                .orElseThrow(() -> ApiException.unauthorized(
                        "ログイン中の社員が見つかりません。再度ログインしてください"));

        ImportJob job = new ImportJob();
        job.setJobId(採番する());
        job.setExecutedBy(実行者);
        job.setFileName(file.getOriginalFilename());
        job.setStatus(ImportJob.Status.PROCESSING);
        job.setTotalRows(rows.size());
        job.setStartedAt(OffsetDateTime.now());
        // 非同期側がジョブを読めるよう、先に確定させる
        importJobRepository.saveAndFlush(job);

        importWorker.run(job.getJobId(), rows);
        return job;
    }

    /** 取込結果の取得（API 10.2）。 */
    @Transactional(readOnly = true)
    public ImportJob findJob(String jobId) {
        return importJobRepository.findById(jobId)
                .orElseThrow(() -> ApiException.notFound("指定されたジョブが見つかりません"));
    }

    /**
     * ヘッダーを検証しつつデータ行を読み込む。
     *
     * <p>文字コードは UTF-8。BOM は先頭から取り除く（仕様書 10.1 は
     * BOM の有無どちらも可としている）。改行は CRLF / LF の両方に対応する
     * （{@code BufferedReader#readLine} がどちらも扱う）。
     */
    private List<String[]> 読み込む(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.IMPORT_FILE_INVALID, "CSVファイルを指定してください");
        }

        List<String[]> rows = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new ApiException(ErrorCode.IMPORT_FILE_INVALID, "CSVが空です");
            }
            検証ヘッダー(BOMを除く(headerLine));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                rows.add(分割する(line));
            }
        } catch (IOException e) {
            // 文字コード不正などで読めない場合もここに来る
            throw new ApiException(ErrorCode.IMPORT_FILE_INVALID,
                    "CSVを読み取れませんでした。文字コードがUTF-8か確認してください");
        }

        return rows;
    }

    private String BOMを除く(String line) {
        return line.startsWith("﻿") ? line.substring(1) : line;
    }

    private void 検証ヘッダー(String headerLine) {
        List<String> actual = Arrays.stream(分割する(headerLine)).map(String::trim).toList();

        if (!actual.equals(CSV_HEADER)) {
            throw new ApiException(ErrorCode.IMPORT_FILE_INVALID,
                    "1行目のヘッダーが期待と異なります。期待する並び: "
                            + String.join(",", CSV_HEADER));
        }
    }

    /**
     * CSVの1行を分割する。
     *
     * <p>ダブルクォートで囲まれた値の中のカンマに対応している。
     * 自己PRや住所にカンマが入ることがあるため、単純な {@code split(",")} では足りない。
     */
    static String[] 分割する(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean 引用符の中 = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                // 連続する "" は値としての " を表す
                if (引用符の中 && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    引用符の中 = !引用符の中;
                }
            } else if (c == ',' && !引用符の中) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());

        return values.toArray(new String[0]);
    }

    /**
     * ジョブIDを採番する（{@code imp_20260727_001} 形式）。
     *
     * <p>当日の件数+1 を連番にしている。
     */
    private String 採番する() {
        String prefix = "imp_" + LocalDate.now().format(JOB_ID_DATE) + "_";
        long 当日の件数 = importJobRepository.countByJobIdStartingWith(prefix);
        return prefix + "%03d".formatted(当日の件数 + 1);
    }

    /**
     * 1行を社員の登録内容に変換する。
     *
     * <p>列が足りない・数値や日付として読めない場合はその行のエラーにする。
     */
    static EmployeeUpsertRequest 行を変換する(String[] columns) {
        if (columns.length < CSV_HEADER.size()) {
            throw new IllegalArgumentException(
                    "列数が足りません（%d列、必要なのは%d列）"
                            .formatted(columns.length, CSV_HEADER.size()));
        }

        return new EmployeeUpsertRequest(
                空文字をnullに(columns[0]),
                空文字をnullに(columns[1]),
                空文字をnullに(columns[2]),
                空文字をnullに(columns[3]),
                空文字をnullに(columns[4]),
                日付(columns[5], "hire_date"),
                数値(columns[6], "department_id"),
                数値(columns[7], "position_id"),
                数値(columns[8], "employment_type_id"),
                数値(columns[9], "seat_id"));
    }

    private static String 空文字をnullに(String value) {
        String trimmed = value == null ? null : value.trim();
        return trimmed == null || trimmed.isEmpty() ? null : trimmed;
    }

    private static LocalDate 日付(String value, String 項目) {
        String v = 空文字をnullに(value);
        if (v == null) {
            return null;
        }
        try {
            return LocalDate.parse(v);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(
                    "%s は YYYY-MM-DD の形式で指定してください".formatted(項目));
        }
    }

    private static Long 数値(String value, String 項目) {
        String v = 空文字をnullに(value);
        if (v == null) {
            return null;
        }
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("%s は数値で指定してください".formatted(項目));
        }
    }

    /**
     * CSV出力（API 10.3）。
     *
     * <p>検索条件は 2.1 と同じものを受け付け、{@code page} / {@code per_page} は
     * 無効で全件出力する。
     *
     * <p>Excel対応のため <b>UTF-8 BOM付き</b>で出力する。BOM が無いと Excel が
     * Shift_JIS と解釈して日本語が化ける。
     *
     * <p>ID ではなく名称（部署名・役職名など）を出している。人が開いて確認・
     * 編集するためのファイルなので、IDだけでは読めない。
     * なお取込側（10.1）は ID を受け取る形なので、<b>出力したCSVをそのまま
     * 取込に使うことはできない</b>。仕様書がそれぞれ別の形を定めているため。
     */
    @Transactional(readOnly = true)
    public String エクスポートする(EmployeeSearchCriteria criteria) {
        List<Employee> employees = employeeRepository.findAll(
                EmployeeSpecifications.from(criteria),
                Sort.by(Sort.Direction.ASC, "employeeCode"));

        StringBuilder csv = new StringBuilder();
        // Excel が UTF-8 と判定できるように BOM を先頭に付ける
        csv.append('﻿');
        csv.append(String.join(",", EXPORT_HEADER)).append("\r\n");

        for (Employee e : employees) {
            csv.append(CSVの行にする(e)).append("\r\n");
        }
        return csv.toString();
    }

    /** CSV出力のヘッダー。取込用（CSV_HEADER）とは別で、IDではなく名称を出す */
    static final List<String> EXPORT_HEADER = List.of(
            "employee_code", "name", "name_kana", "email", "tel",
            "hire_date", "department_name", "position_name",
            "employment_type_name", "seat_number");

    /** CSV出力の1行を組み立てる（API 10.3）。 */
    static String CSVの行にする(Employee e) {
        return String.join(",", List.of(
                 引用する(e.getEmployeeCode()),
                 引用する(e.getName()),
                 引用する(e.getNameKana()),
                 引用する(e.getEmail()),
                 引用する(e.getTel()),
                 引用する(String.valueOf(e.getHireDate())),
                 引用する(e.getDepartment() == null ? null : e.getDepartment().getDepartmentName()),
                 引用する(e.getPosition() == null ? null : e.getPosition().getPositionName()),
                 引用する(e.getEmploymentType() == null
                        ? null : e.getEmploymentType().getEmploymentTypeName()),
                 引用する(e.getSeat() == null ? null : e.getSeat().getSeatNumber())));
    }

    /**
     * CSVの値として引用する。
     *
     * <p>カンマ・ダブルクォート・改行を含む値はダブルクォートで囲む。
     * 氏名や住所にカンマが入ると列がずれるため。
     */
    static String 引用する(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
