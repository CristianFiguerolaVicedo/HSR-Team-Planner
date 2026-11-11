import { NavLink, Route, Routes } from "react-router-dom";
import CatalogPage from "./pages/CatalogPage";
import RosterPage from "./pages/RosterPage";
import TeamBuilderPage from "./pages/TeamBuilderPage";
import SharedTeamPage from "./pages/SharedTeamPage";
import SpdCalculatorPage from "./pages/SpdCalculatorPage";

const links = [
  { to: "/", label: "Catalog" },
  { to: "/roster", label: "My characters" },
  { to: "/builder", label: "Team builder" },
  { to: "/spd", label: "SPD calculator" },
];

export default function App() {
  return (
    <div className="min-h-screen">
      <nav className="flex gap-4 p-4">
        {links.map((l) => (
          <NavLink key={l.to} to={l.to} end>
            {l.label}
          </NavLink>
        ))}
      </nav>
      <main className="p-4">
        <Routes>
          <Route path="/" element={<CatalogPage />} />
          <Route path="/roster" element={<RosterPage />} />
          <Route path="/builder" element={<TeamBuilderPage />} />
          <Route path="/spd" element={<SpdCalculatorPage />} />
          {/* Share links: /t/aZ3kP9qX */}
          <Route path="/t/:shareCode" element={<SharedTeamPage />} />
        </Routes>
      </main>
    </div>
  );
}
