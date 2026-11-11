import { useParams } from "react-router-dom";

// TODO: read-only view of a shared team (getTeam(shareCode) in services/plannerService.js)
const SharedTeam = () => {
  const { shareCode } = useParams();
  return (
    <div className="text-text-primary">
      <h1 className="text-5xl md:text-6xl font-extrabold tracking-tight">Shared team</h1>
      <p className="font-hand text-2xl text-text-secondary mt-1">ticket {shareCode}</p>
    </div>
  );
};

export default SharedTeam;
