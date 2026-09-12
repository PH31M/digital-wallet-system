export function maskWalletId(id: string): string {
  const digitsOrChars = id.replace(/-/g, '');
  return `•••• ${digitsOrChars.slice(-4).toUpperCase()}`;
}
