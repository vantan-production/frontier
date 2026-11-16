package jp.co.frontier.ems.dto;

import jakarta.validation.constraints.Size;

/**
 * 自己プロフィール更新のリクエスト（API 5.1）。
 *
 * <p>いずれも任意項目で、<b>指定した項目だけ</b>更新する（PATCH の意味どおり）。
 *
 * <p>JSON に項目が無い場合と {@code null} を明示した場合は Jackson 上で
 * 区別できないため、<b>どちらも「更新しない」として扱う</b>。
 * 値を消したい場合は空文字 {@code ""} を送る。
 *
 * <p>編集できるのは {@code hobby} / {@code self_pr} / 顔写真のみ。
 * それ以外の基本情報は変更申請（API 9章）を通す。
 *
 * @param hobby  趣味。DBが VARCHAR(255) なので上限もそれに合わせる
 * @param selfPr 自己PR。DBは TEXT だが無制限だと際限がないため上限を設けている
 */
public record ProfileUpdateRequest(
        @Size(max = 255, message = "趣味は255文字以内で入力してください")
        String hobby,

        @Size(max = 2000, message = "自己PRは2000文字以内で入力してください")
        String selfPr) {
}
