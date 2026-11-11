import { useParams } from "react-router-dom";

// TODO: read-only view of a shared team. Data: api.team(shareCode)
export default function SharedTeamPage() {
  const { shareCode } = useParams<{ shareCode: string }>();
  return <h1 className="text-2xl font-semibold">Shared team {shareCode}</h1>;
}
