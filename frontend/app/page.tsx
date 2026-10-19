
"use client";

import { useEffect, useMemo, useState } from "react";

type Employee = {
  id: number;
  name: string;
  role: string;
  dept: string;
  floor: string;
  seat: string;
  year: string;
  skills: string[];
  pr: string;
  status: "在席" | "外出中" | "離席中";
};

const employees: Employee[] = [
  {
    id: 1,
    name: "山田 太郎",
    role: "シニアエンジニア",
    dept: "開発部",
    floor: "本社 3F",
    seat: "A-14",
    year: "2017",
    skills: ["Java", "AWS", "Docker", "SQL"],
    pr: "山岳部所属。休日は登山でリフレッシュ。大規模システムの設計が得意です。",
    status: "在席",
  },
  {
    id: 2,
    name: "佐藤 花子",
    role: "プロジェクトマネージャー",
    dept: "営業部",
    floor: "本社 4F",
    seat: "B-07",
    year: "2015",
    skills: ["PMP", "Python", "SQL", "Excel"],
    pr: "料理とランニングが趣味。チームのまとめ役として日々奮闘中！",
    status: "外出中",
  },
  {
    id: 3,
    name: "鈴木 一郎",
    role: "インフラエンジニア",
    dept: "開発部",
    floor: "技術センター 1F",
    seat: "C-03",
    year: "2019",
    skills: ["AWS", "Docker", "Linux", "Terraform"],
    pr: "インフラの安定稼働を守るのが使命です。",
    status: "在席",
  },
  {
    id: 4,
    name: "田中 めぐみ",
    role: "UIデザイナー",
    dept: "マーケティング部",
    floor: "本社 3F",
    seat: "A-09",
    year: "2020",
    skills: ["Figma", "React", "CSS", "Adobe XD"],
    pr: "音楽とデザインが好き。使いやすいUI設計を追求しています。",
    status: "在席",
  },
  {
    id: 5,
    name: "伊藤 健",
    role: "営業リーダー",
    dept: "営業部",
    floor: "本社 4F",
    seat: "B-12",
    year: "2014",
    skills: ["営業", "Excel", "Salesforce"],
    pr: "お客様との信頼構築を大切にしています。",
    status: "在席",
  },
  {
    id: 6,
    name: "小林 さくら",
    role: "人事担当",
    dept: "人事部",
    floor: "本社 5F",
    seat: "D-01",
    year: "2018",
    skills: ["採用", "労務", "Excel"],
    pr: "社員のキャリア支援に力を入れています。",
    status: "離席中",
  },
  {
    id: 7,
    name: "中村 浩二",
    role: "Webエンジニア",
    dept: "開発部",
    floor: "技術センター 2F",
    seat: "C-11",
    year: "2021",
    skills: ["React", "Node.js", "TypeScript"],
    pr: "フロントエンドからバックエンドまで幅広く対応します。",
    status: "在席",
  },
  {
    id: 8,
    name: "高橋 美咲",
    role: "経営企画担当",
    dept: "経営企画室",
    floor: "本社 5F",
    seat: "D-05",
    year: "2016",
    skills: ["Excel", "SQL", "財務分析"],
    pr: "データから経営の意思決定をサポートします。",
    status: "外出中",
  },
  {
    id: 9,
    name: "渡辺 剛",
    role: "シニアエンジニア",
    dept: "開発部",
    floor: "本社 3F",
    seat: "A-18",
    year: "2013",
    skills: ["Java", "Spring Boot", "Oracle"],
    pr: "技術力でビジネスの課題を解決するのが好きです。",
    status: "在席",
  },
  {
    id: 10,
    name: "松本 えり",
    role: "マーケター",
    dept: "マーケティング部",
    floor: "本社 4F",
    seat: "B-04",
    year: "2022",
    skills: ["SEO", "Google Ads", "Analytics"],
    pr: "データを活用したマーケティングに取り組んでいます。",
    status: "在席",
  },
  {
    id: 11,
    name: "加藤 龍一",
    role: "総務リーダー",
    dept: "総務部",
    floor: "本社 5F",
    seat: "D-08",
    year: "2010",
    skills: ["施設管理", "法務", "Excel"],
    pr: "縁の下の力持ちとして会社を支えます。",
    status: "在席",
  },
  {
    id: 12,
    name: "井上 さやか",
    role: "フロントエンドエンジニア",
    dept: "開発部",
    floor: "技術センター 1F",
    seat: "C-07",
    year: "2020",
    skills: ["Vue.js", "TypeScript", "CSS", "Figma"],
    pr: "使いやすいUI作りに情熱を燃やしています。",
    status: "在席",
  },
  {
    id: 13,
    name: "吉田 直樹",
    role: "バックエンドエンジニア",
    dept: "開発部",
    floor: "技術センター 2F",
    seat: "C-14",
    year: "2018",
    skills: ["Java", "Spring Boot", "SQL"],
    pr: "保守しやすく安定したシステムづくりを心がけています。",
    status: "在席",
  },
  {
    id: 14,
    name: "山口 葵",
    role: "クラウドエンジニア",
    dept: "開発部",
    floor: "技術センター 1F",
    seat: "C-16",
    year: "2021",
    skills: ["AWS", "Linux", "Docker"],
    pr: "クラウド環境の構築と自動化が得意です。",
    status: "外出中",
  },
  {
    id: 15,
    name: "斎藤 拓海",
    role: "営業担当",
    dept: "営業部",
    floor: "本社 4F",
    seat: "B-15",
    year: "2022",
    skills: ["営業", "Excel", "Salesforce"],
    pr: "相手の立場に立った提案を大切にしています。",
    status: "在席",
  },
  {
    id: 16,
    name: "松井 玲奈",
    role: "採用担当",
    dept: "人事部",
    floor: "本社 5F",
    seat: "D-03",
    year: "2019",
    skills: ["採用", "労務", "Excel"],
    pr: "一人ひとりが力を発揮できる環境づくりを目指しています。",
    status: "在席",
  },
  {
    id: 17,
    name: "池田 翔",
    role: "ネットワークエンジニア",
    dept: "開発部",
    floor: "技術センター 2F",
    seat: "C-19",
    year: "2016",
    skills: ["Linux", "ネットワーク", "AWS"],
    pr: "ネットワークの設計とトラブル対応を担当しています。",
    status: "離席中",
  },
  {
    id: 18,
    name: "阿部 美月",
    role: "Webデザイナー",
    dept: "マーケティング部",
    floor: "本社 3F",
    seat: "A-21",
    year: "2023",
    skills: ["Figma", "CSS", "Adobe XD"],
    pr: "伝わりやすく心地よいデザインを追求しています。",
    status: "在席",
  },
  {
    id: 19,
    name: "森 大輔",
    role: "データアナリスト",
    dept: "経営企画室",
    floor: "本社 5F",
    seat: "D-11",
    year: "2017",
    skills: ["SQL", "Python", "Excel"],
    pr: "データを整理し、意思決定に役立つ形で届けます。",
    status: "在席",
  },
  {
    id: 20,
    name: "石川 千尋",
    role: "総務担当",
    dept: "総務部",
    floor: "本社 5F",
    seat: "D-13",
    year: "2020",
    skills: ["施設管理", "Excel", "法務"],
    pr: "社員が働きやすい職場環境を整えています。",
    status: "外出中",
  },
  {
    id: 21,
    name: "前田 悠人",
    role: "QAエンジニア",
    dept: "開発部",
    floor: "技術センター 2F",
    seat: "C-22",
    year: "2022",
    skills: ["テスト", "SQL", "Python"],
    pr: "品質を高めるためのテスト設計に取り組んでいます。",
    status: "在席",
  },
  {
    id: 22,
    name: "藤田 莉子",
    role: "マーケティング担当",
    dept: "マーケティング部",
    floor: "本社 4F",
    seat: "B-19",
    year: "2021",
    skills: ["SEO", "Analytics", "Google Ads"],
    pr: "ユーザーの声をもとに情報発信を改善しています。",
    status: "在席",
  },
  {
    id: 23,
    name: "岡本 健太",
    role: "社内SE",
    dept: "開発部",
    floor: "本社 3F",
    seat: "A-24",
    year: "2015",
    skills: ["Windows", "Linux", "ネットワーク"],
    pr: "社内システムの安定運用と問い合わせ対応をしています。",
    status: "離席中",
  },
  {
    id: 24,
    name: "長谷川 愛",
    role: "経理担当",
    dept: "経営企画室",
    floor: "本社 5F",
    seat: "D-16",
    year: "2018",
    skills: ["Excel", "財務分析", "SQL"],
    pr: "数字を正確に管理し、事業運営を支えています。",
    status: "在席",
  },
  {
    id: 25,
    name: "後藤 陸",
    role: "アプリエンジニア",
    dept: "開発部",
    floor: "技術センター 1F",
    seat: "C-25",
    year: "2023",
    skills: ["TypeScript", "React", "Node.js"],
    pr: "日々学びながら、便利な機能の開発に挑戦しています。",
    status: "在席",
  },
  {
    id: 26,
    name: "近藤 由佳",
    role: "営業アシスタント",
    dept: "営業部",
    floor: "本社 4F",
    seat: "B-22",
    year: "2020",
    skills: ["Excel", "Salesforce", "資料作成"],
    pr: "チームがスムーズに動けるよう先回りしてサポートします。",
    status: "外出中",
  },
  {
    id: 27,
    name: "石井 颯太",
    role: "セキュリティエンジニア",
    dept: "開発部",
    floor: "技術センター 2F",
    seat: "C-28",
    year: "2019",
    skills: ["Linux", "AWS", "セキュリティ"],
    pr: "安全にサービスを利用できる環境づくりを担当しています。",
    status: "在席",
  },
  {
    id: 28,
    name: "清水 ひかり",
    role: "広報担当",
    dept: "マーケティング部",
    floor: "本社 3F",
    seat: "A-27",
    year: "2024",
    skills: ["SNS運用", "SEO", "Analytics"],
    pr: "社内外に会社の魅力をわかりやすく発信しています。",
    status: "在席",
  },
  {
    id: 29,
    name: "福田 誠",
    role: "プロジェクトリーダー",
    dept: "開発部",
    floor: "本社 3F",
    seat: "A-30",
    year: "2012",
    skills: ["Java", "AWS", "PMP"],
    pr: "チームで相談しながら着実にプロジェクトを進めます。",
    status: "在席",
  },
  {
    id: 30,
    name: "村上 奈々",
    role: "人事企画担当",
    dept: "人事部",
    floor: "本社 5F",
    seat: "D-19",
    year: "2022",
    skills: ["採用", "Excel", "労務"],
    pr: "社員の成長につながる制度づくりに取り組んでいます。",
    status: "在席",
  },
];

const departments = [
  "すべての部署",
  "開発部",
  "営業部",
  "人事部",
  "総務部",
  "マーケティング部",
  "経営企画室",
];

const colors = [
  "#2A5FA0",
  "#C24E1A",
  "#1B6B45",
  "#7B4CA0",
  "#1E7DA0",
  "#8A3A3A",
];

const departmentColors: Record<string, string> = {
  開発部: "#2A5FA0",
  営業部: "#C24E1A",
  人事部: "#1B6B45",
  総務部: "#7A5800",
  マーケティング部: "#7B4CA0",
  経営企画室: "#54504A",
};

function avatarColor(name: string) {
  const sum = [...name].reduce((total, char) => total + char.charCodeAt(0), 0);
  return colors[sum % colors.length];
}

function initials(name: string) {
  return name.replace(/\s/g, "").slice(0, 2);
}

export default function Home() {
  const [keyword, setKeyword] = useState("");
  const [department, setDepartment] = useState("すべての部署");
  const [selectedSkills, setSelectedSkills] = useState<string[]>([]);
  const [view, setView] = useState<"grid" | "list">("grid");
  const [activeTab, setActiveTab] = useState("全社員");
  const [selectedEmployee, setSelectedEmployee] =
    useState<Employee | null>(null);
  const [page, setPage] = useState("search");
  const [mobileMenu, setMobileMenu] = useState(false);
  const [currentPage, setCurrentPage] = useState(1);
  const ITEMS_PER_PAGE = 10;

  const skillOptions = [
    "Java",
    "Python",
    "React",
    "AWS",
    "PMP",
    "応用情報",
    "SQL",
    "Docker",
  ];

  const filteredEmployees = useMemo(() => {
    const query = keyword.trim().toLowerCase();

    return employees.filter((employee) => {
      const matchesKeyword =
        !query ||
        [
          employee.name,
          employee.role,
          employee.dept,
          employee.floor,
          ...employee.skills,
        ]
          .join(" ")
          .toLowerCase()
          .includes(query);

      const matchesDepartment =
        department === "すべての部署" ||
        employee.dept === department;

      const matchesSkills =
        selectedSkills.length === 0 ||
        selectedSkills.every((skill) => employee.skills.includes(skill));

      const matchesTab =
        activeTab === "全社員" ||
        (activeTab === "本社" && employee.floor.startsWith("本社")) ||
        (activeTab === "技術センター" &&
          employee.floor.startsWith("技術センター")) ||
        (activeTab === "他拠点" &&
          !employee.floor.startsWith("本社") &&
          !employee.floor.startsWith("技術センター"));

      return (
        matchesKeyword &&
        matchesDepartment &&
        matchesSkills &&
        matchesTab
      );
    });
  }, [keyword, department, selectedSkills, activeTab]);

  // 検索条件やタブが変わったら1ページ目に戻す
  useEffect(() => {
    setCurrentPage(1);
  }, [keyword, department, selectedSkills, activeTab]);

  const totalPages = Math.max(
    1,
    Math.ceil(filteredEmployees.length / ITEMS_PER_PAGE)
  );

  const paginatedEmployees = filteredEmployees.slice(
    (currentPage - 1) * ITEMS_PER_PAGE,
    currentPage * ITEMS_PER_PAGE
  );

  function changePage(nextPage: number) {
    const safePage = Math.min(Math.max(nextPage, 1), totalPages);
    setCurrentPage(safePage);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function toggleSkill(skill: string) {
    setSelectedSkills((current) =>
      current.includes(skill)
        ? current.filter((item) => item !== skill)
        : [...current, skill]
    );
  }

  function resetFilters() {
    setKeyword("");
    setDepartment("すべての部署");
    setSelectedSkills([]);
    setActiveTab("全社員");
  }

  const navItems = [
    { id: "search", label: "社員検索", icon: "" },
    { id: "floor", label: "フロアマップ", icon: "" },
    { id: "thanks", label: "サンクス", icon: "" },
    { id: "profile", label: "マイページ", icon: "" },
    { id: "requests", label: "申請", icon: "" },
    { id: "admin", label: "管理", icon: "" },
  ];

  return (
    <div className="min-h-screen bg-[#F4F1EC] text-[#1A1612]">
      {/* ヘッダー */}
      <header className="sticky top-0 z-30 flex h-[54px] items-center gap-4 border-b border-white/[0.06] bg-[#16120E] px-5 text-white">
        <button
          className="shrink-0 p-1 text-xl text-white/70 md:hidden"
          onClick={() => setMobileMenu(!mobileMenu)}
          aria-label="メニューを開く"
        >
          ☰
        </button>

        <button
          onClick={() => setPage("search")}
          className="shrink-0 font-serif text-[19px] tracking-wide text-[#F0EDE8]"
        >
          Frontier<span className="text-[#6FA8F0]">.</span>
        </button>

        <div className="relative w-full max-w-[420px] flex-1">
          <span className="pointer-events-none absolute left-[10px] top-1/2 -translate-y-1/2 text-sm opacity-50">
            🔍
          </span>
          <input
            value={keyword}
            onChange={(event) => {
              setKeyword(event.target.value);
              setPage("search");
            }}
            placeholder="社員名・スキル・部署で検索..."
            className="h-[34px] w-full rounded-md border border-white/[0.14] bg-white/[0.09] pl-[34px] pr-3 text-[13px] text-[#F0EDE8] outline-none placeholder:text-white/40 focus:border-white/30 focus:bg-white/[0.14]"
          />
        </div>

        <nav className="ml-2 hidden items-center gap-0.5 lg:flex">
          {navItems.map((item) => (
            <button
              key={item.id}
              onClick={() => setPage(item.id)}
              className={`flex items-center gap-[5px] whitespace-nowrap rounded-md px-3 py-1.5 text-[12.5px] transition ${
                page === item.id
                  ? "bg-[#6FA8F0]/[0.22] text-[#F0EDE8]"
                  : "text-white/55 hover:bg-white/[0.07] hover:text-[#F0EDE8]"
              }`}
            >
              <span className="text-sm">{item.icon}</span> {item.label}
            </button>
          ))}
        </nav>

        <div className="ml-auto flex shrink-0 items-center gap-2">
          <button
            onClick={() => setPage("profile")}
            aria-label="通知"
            className="relative flex h-8 w-8 shrink-0 items-center justify-center rounded-full border border-white/[0.13] text-[15px] text-white/60 transition hover:bg-white/10 hover:text-white"
          >
            ♧
            <span className="absolute right-[5px] top-[5px] h-[7px] w-[7px] rounded-full border-2 border-[#16120E] bg-[#E86C3A]" />
          </button>

          <button
            onClick={() => setPage("profile")}
            aria-label="マイページ"
            className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-[#6FA8F0] to-[#3A6DB0] text-[11px] font-bold text-white"
          >
            鈴
          </button>
        </div>
      </header>

      {/* モバイルメニュー */}
      {mobileMenu && (
        <div className="fixed inset-0 z-40 bg-black/40 md:hidden">
          <nav className="h-full w-72 bg-[#16120E] p-5 text-white">
            <div className="mb-6 flex items-center justify-between">
              <span className="font-serif text-xl">
                Frontier<span className="text-[#6FA8F0]">.</span>
              </span>
              <button onClick={() => setMobileMenu(false)}>✕</button>
            </div>
            {navItems.map((item) => (
              <button
                key={item.id}
                onClick={() => {
                  setPage(item.id);
                  setMobileMenu(false);
                }}
                className="mb-2 block w-full rounded-md p-3 text-left text-sm hover:bg-white/10"
              >
                {item.icon}　{item.label}
              </button>
            ))}
          </nav>
        </div>
      )}

      <div className="flex min-h-[calc(100vh-58px)]">
        {/* 左サイドバー */}
        {page === "search" && (
          <aside className="sticky top-[58px] hidden h-[calc(100vh-58px)] w-[250px] shrink-0 flex-col gap-5 overflow-y-auto border-r border-black/10 bg-white p-5 lg:flex">
            <p className="text-[10px] font-bold tracking-[0.15em] text-[#96908A]">
              絞り込み検索
            </p>

            <div>
              <label className="mb-2 block text-xs text-[#54504A]">
                氏名・キーワード
              </label>
              <input
                value={keyword}
                onChange={(event) => setKeyword(event.target.value)}
                placeholder="例：山田、AWS..."
                className="w-full rounded-md border border-black/20 bg-[#F4F1EC] px-3 py-2 text-sm outline-none focus:border-[#1E52A0]"
              />
            </div>

            <div>
              <label className="mb-2 block text-xs text-[#54504A]">
                所属部署
              </label>
              <select
                value={department}
                onChange={(event) => setDepartment(event.target.value)}
                className="w-full rounded-md border border-black/20 bg-[#F4F1EC] px-3 py-2 text-sm outline-none focus:border-[#1E52A0]"
              >
                {departments.map((dept) => (
                  <option key={dept}>{dept}</option>
                ))}
              </select>
            </div>

            <div>
              <label className="mb-2 block text-xs text-[#54504A]">
                スキル・資格
              </label>
              <div className="flex flex-wrap gap-2">
                {skillOptions.map((skill) => (
                  <button
                    key={skill}
                    onClick={() => toggleSkill(skill)}
                    className={`rounded-full border px-2.5 py-1 text-[11px] transition ${
                      selectedSkills.includes(skill)
                        ? "border-[#1E52A0] bg-[#EAF1FB] text-[#1E52A0]"
                        : "border-black/20 bg-white text-[#54504A] hover:border-[#1E52A0]"
                    }`}
                  >
                    {skill}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="mb-2 block text-xs text-[#54504A]">
                フロア
              </label>
              <select
                onChange={(event) => {
                  const value = event.target.value;
                  setKeyword(value === "すべてのフロア" ? "" : value);
                }}
                className="w-full rounded-md border border-black/20 bg-[#F4F1EC] px-3 py-2 text-sm outline-none"
              >
                <option>すべてのフロア</option>
                <option>本社 3F</option>
                <option>本社 4F</option>
                <option>本社 5F</option>
                <option>技術センター 1F</option>
                <option>技術センター 2F</option>
              </select>
            </div>

            <button
              onClick={() => setPage("search")}
              className="rounded-md bg-[#1E52A0] py-2.5 text-sm text-white transition hover:bg-[#153D7A]"
            >
              検索する
            </button>
            <button
              onClick={resetFilters}
              className="rounded-md border border-black/20 py-2 text-sm text-[#96908A] hover:bg-[#F4F1EC]"
            >
              リセット
            </button>
          </aside>
        )}

        {/* メインコンテンツ */}
        <main className="min-w-0 flex-1 p-4 sm:p-6">
          {page === "search" ? (
            <>
              {/* 統計カード */}
              <section className="mb-5 grid grid-cols-2 gap-3 xl:grid-cols-4">
                {[
                  { number: "812", label: "総社員数", note: "↑ 先月比 +5名" },
                  { number: "23", label: "部署数", note: "" },
                  { number: "5", label: "拠点数", note: "" },
                  { number: "147", label: "今月のサンクス", note: "今週 +32" },
                ].map((stat) => (
                  <div
                    key={stat.label}
                    className="rounded-xl border border-black/[0.08] bg-white p-4 shadow-sm"
                  >
                    <p className="font-mono text-2xl font-medium">
                      {stat.number}
                    </p>
                    <p className="mt-1 text-[11px] tracking-wide text-[#96908A]">
                      {stat.label}
                    </p>
                    {stat.note && (
                      <p className="mt-1 text-[11px] text-[#176640]">
                        {stat.note}
                      </p>
                    )}
                  </div>
                ))}
              </section>

              {/* 拠点タブ */}
              <div className="mb-5 flex gap-5 overflow-x-auto border-b border-black/10">
                {["全社員", "技術センター", "本社", "他拠点"].map((tab) => (
                  <button
                    key={tab}
                    onClick={() => setActiveTab(tab)}
                    className={`whitespace-nowrap border-b-2 px-1 pb-3 text-sm ${
                      activeTab === tab
                        ? "border-[#1E52A0] font-medium text-[#1E52A0]"
                        : "border-transparent text-[#96908A] hover:text-[#1A1612]"
                    }`}
                  >
                    {tab === "全社員" ? "👥" : tab === "本社" ? "🏢" : "🌐"}{" "}
                    {tab}
                  </button>
                ))}
              </div>

              {/* 一覧ヘッダー */}
              <section className="mb-4 flex flex-wrap items-end justify-between gap-3">
                <div>
                  <h1 className="font-serif text-2xl">社員一覧</h1>
                  <p className="mt-1 text-xs text-[#96908A]">
                    検索条件に合致する社員 —{" "}
                    <span className="font-bold text-[#1A1612]">
                      {filteredEmployees.length}名
                    </span>
                    を表示中
                  </p>
                </div>

                <div className="flex overflow-hidden rounded-md border border-black/20">
                  <button
                    onClick={() => setView("grid")}
                    className={`px-3 py-1.5 ${
                      view === "grid"
                        ? "bg-[#1E52A0] text-white"
                        : "bg-white text-[#96908A]"
                    }`}
                    aria-label="カード表示"
                  >
                    ⊞
                  </button>
                  <button
                    onClick={() => setView("list")}
                    className={`px-3 py-1.5 ${
                      view === "list"
                        ? "bg-[#1E52A0] text-white"
                        : "bg-white text-[#96908A]"
                    }`}
                    aria-label="リスト表示"
                  >
                    ≡
                  </button>
                </div>
              </section>

              {/* 社員カード・リスト */}
              <section
                className={
                  view === "grid"
                    ? "grid grid-cols-2 gap-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5"
                    : "flex flex-col gap-2"
                }
              >
                {paginatedEmployees.map((employee) => (
                  <button
                    key={employee.id}
                    onClick={() => setSelectedEmployee(employee)}
                    className={`relative overflow-hidden rounded-xl border border-black/[0.08] bg-white text-left shadow-sm transition hover:-translate-y-0.5 hover:border-black/20 hover:shadow-md ${
                      view === "grid"
                        ? "flex flex-col items-center gap-1.5 px-2 py-2.5 text-center"
                        : "flex items-center gap-3 p-2.5"
                    }`}
                  >
                    <span
                      className="absolute left-0 top-0 h-[3px] w-full"
                      style={{
                        backgroundColor:
                          departmentColors[employee.dept] ?? "#96908A",
                      }}
                    />

                    <span
                      className={`flex shrink-0 items-center justify-center rounded-full text-lg font-bold text-white ${
                        view === "grid" ? "h-9 w-9 text-sm" : "h-10 w-10 text-base"
                      }`}
                      style={{ backgroundColor: avatarColor(employee.name) }}
                    >
                      {initials(employee.name)}
                    </span>

                    <span className={view === "grid" ? "w-full" : "min-w-0 flex-1"}>
                      <span className="block text-[12px] font-bold leading-5">
                        {employee.name}
                      </span>
                      <span className="mt-0.5 block text-[10px] leading-4 text-[#96908A]">
                        {employee.role}
                      </span>
                      <span
                        className="mt-1 inline-block rounded-full px-2 py-0.5 text-[9px] font-semibold"
                        style={{
                          color: departmentColors[employee.dept],
                          backgroundColor: `${departmentColors[employee.dept]}15`,
                        }}
                      >
                        {employee.dept}
                      </span>
                    </span>

                    <span
                      className={`flex items-center gap-1 text-[10px] text-[#96908A] ${
                        view === "list" ? "shrink-0" : ""
                      }`}
                    >
                      <span
                        className={`h-1.5 w-1.5 rounded-full ${
                          employee.status === "在席"
                            ? "bg-[#34C06B]"
                            : employee.status === "外出中"
                              ? "bg-[#E07040]"
                              : "bg-[#C8C4BC]"
                        }`}
                      />
                      {employee.status} · {employee.floor}
                    </span>

                    <span
                      className={`flex flex-wrap gap-1 ${
                        view === "grid" ? "justify-center" : "hidden sm:flex"
                      }`}
                    >
                      {employee.skills.slice(0, 3).map((skill) => (
                        <span
                          key={skill}
                          className="rounded border border-black/[0.08] bg-[#F4F1EC] px-1.5 py-0.5 text-[9px] text-[#54504A]"
                        >
                          {skill}
                        </span>
                      ))}
                    </span>
                  </button>
                ))}
              </section>

              {filteredEmployees.length === 0 && (
                <div className="rounded-xl border border-dashed border-black/20 bg-white/50 py-16 text-center text-sm text-[#96908A]">
                  条件に一致する社員が見つかりませんでした。
                  <button
                    onClick={resetFilters}
                    className="ml-2 text-[#1E52A0] underline"
                  >
                    条件をリセット
                  </button>
                </div>
              )}

              {/* ページ切り替え：既存のデザインに合わせた最小限の追加 */}
              {filteredEmployees.length > 0 && (
                <div className="mt-5 flex flex-col items-center gap-3 pb-4">
                  <p className="text-xs text-[#96908A]">
                    {(currentPage - 1) * ITEMS_PER_PAGE + 1}〜
                    {Math.min(currentPage * ITEMS_PER_PAGE, filteredEmployees.length)}件 / 全{filteredEmployees.length}件
                  </p>
                  <div className="flex items-center gap-1.5">
                    <button
                      onClick={() => changePage(currentPage - 1)}
                      disabled={currentPage === 1}
                      className="rounded-md border border-black/20 bg-white px-3 py-2 text-xs text-[#54504A] transition hover:border-[#1E52A0] disabled:cursor-not-allowed disabled:opacity-30"
                    >
                      ← 前へ
                    </button>
                    {Array.from({ length: totalPages }, (_, index) => index + 1).map((pageNumber) => (
                      <button
                        key={pageNumber}
                        onClick={() => changePage(pageNumber)}
                        aria-current={currentPage === pageNumber ? "page" : undefined}
                        className={`h-9 min-w-9 rounded-md border px-3 text-sm transition ${
                          currentPage === pageNumber
                            ? "border-[#1E52A0] bg-[#1E52A0] text-white"
                            : "border-black/20 bg-white text-[#54504A] hover:border-[#1E52A0]"
                        }`}
                      >
                        {pageNumber}
                      </button>
                    ))}
                    <button
                      onClick={() => changePage(currentPage + 1)}
                      disabled={currentPage === totalPages}
                      className="rounded-md border border-black/20 bg-white px-3 py-2 text-xs text-[#54504A] transition hover:border-[#1E52A0] disabled:cursor-not-allowed disabled:opacity-30"
                    >
                      次へ →
                    </button>
                  </div>
                </div>
              )}
            </>
          ) : (
            <section className="rounded-xl border border-black/[0.08] bg-white p-6 sm:p-8">
              <p className="mb-2 text-xs font-bold tracking-widest text-[#1E52A0]">
                FRONTIER / {page.toUpperCase()}
              </p>
              <h1 className="font-serif text-2xl">
                {navItems.find((item) => item.id === page)?.label}
              </h1>
              <p className="mt-3 text-sm leading-7 text-[#96908A]">
                このページはこれから実装できます。社員検索・一覧画面は利用できます。
              </p>
              <button
                onClick={() => setPage("search")}
                className="mt-5 rounded-md bg-[#1E52A0] px-4 py-2 text-sm text-white hover:bg-[#153D7A]"
              >
                社員一覧に戻る
              </button>
            </section>
          )}
        </main>
      </div>

      {/* 社員詳細モーダル */}
      {selectedEmployee && (
        <div
          className="fixed inset-0 z-50 flex justify-end bg-black/30"
          onClick={() => setSelectedEmployee(null)}
        >
          <section
            role="dialog"
            aria-modal="true"
            aria-label="社員プロフィール"
            onClick={(event) => event.stopPropagation()}
            className="h-full w-full max-w-[380px] overflow-y-auto bg-white shadow-2xl"
          >
            <div className="relative flex flex-col items-center border-b border-black/10 px-5 pb-6 pt-10">
              <button
                onClick={() => setSelectedEmployee(null)}
                className="absolute right-4 top-4 flex h-8 w-8 items-center justify-center rounded-full bg-[#F4F1EC] text-[#54504A]"
                aria-label="閉じる"
              >
                ✕
              </button>

              <div
                className="mb-3 flex h-16 w-16 items-center justify-center rounded-full text-xl font-bold text-white"
                style={{
                  backgroundColor: avatarColor(selectedEmployee.name),
                }}
              >
                {initials(selectedEmployee.name)}
              </div>

              <h2 className="font-serif text-xl">{selectedEmployee.name}</h2>
              <p className="mt-1 text-sm text-[#54504A]">
                {selectedEmployee.role}
              </p>
              <span
                className="mt-3 rounded-full px-3 py-1 text-xs font-semibold"
                style={{
                  color: departmentColors[selectedEmployee.dept],
                  backgroundColor: `${departmentColors[selectedEmployee.dept]}15`,
                }}
              >
                {selectedEmployee.dept}
              </span>
            </div>

            <div className="space-y-6 p-5">
              <div>
                <h3 className="mb-3 text-[10px] font-bold tracking-widest text-[#96908A]">
                  基本情報
                </h3>
                {[
                  ["所属部署", selectedEmployee.dept],
                  ["勤務地", selectedEmployee.floor],
                  ["座席番号", selectedEmployee.seat],
                  ["入社年", `${selectedEmployee.year}年`],
                  ["現在の状態", selectedEmployee.status],
                ].map(([label, value]) => (
                  <div
                    key={label}
                    className="flex gap-3 border-b border-black/[0.06] py-2.5 text-sm"
                  >
                    <span className="w-20 shrink-0 text-[#96908A]">
                      {label}
                    </span>
                    <span className="font-medium">{value}</span>
                  </div>
                ))}
              </div>

              <div>
                <h3 className="mb-3 text-[10px] font-bold tracking-widest text-[#96908A]">
                  スキル・資格
                </h3>
                <div className="flex flex-wrap gap-2">
                  {selectedEmployee.skills.map((skill) => (
                    <span
                      key={skill}
                      className="rounded-md border border-[#1E52A0]/20 bg-[#EAF1FB] px-3 py-1.5 text-xs text-[#1E52A0]"
                    >
                      {skill}
                    </span>
                  ))}
                </div>
              </div>

              <div>
                <h3 className="mb-3 text-[10px] font-bold tracking-widest text-[#96908A]">
                  自己PR
                </h3>
                <p className="rounded-md bg-[#F4F1EC] p-4 text-sm leading-7 text-[#54504A]">
                  {selectedEmployee.pr}
                </p>
              </div>

              <button
                onClick={() => {
                  setSelectedEmployee(null);
                  setPage("thanks");
                }}
                className="w-full rounded-md bg-[#1E52A0] py-3 text-sm font-medium text-white transition hover:bg-[#153D7A]"
              >
                サンクスを送る
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}
