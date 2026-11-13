/** Public link for a saved team: https://<site>/t/<shareCode> */
export const shareLink = (shareCode) => `${globalThis.location?.origin ?? ""}/t/${shareCode}`;
