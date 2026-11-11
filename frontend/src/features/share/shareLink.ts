/** Public link for a saved team: https://<site>/t/<shareCode>. */
export function shareLink(shareCode: string): string {
  return `${window.location.origin}/t/${shareCode}`;
}
