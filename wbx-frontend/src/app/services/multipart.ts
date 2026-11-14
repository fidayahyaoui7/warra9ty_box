/** Une partie JSON + des fichiers, comme attendu par @RequestPart côté Spring. */
export function multipart(parts: Record<string, unknown>, files: Record<string, File | null | undefined>): FormData {
  const fd = new FormData();
  for (const [k, v] of Object.entries(parts)) {
    fd.append(k, new Blob([JSON.stringify(v)], { type: 'application/json' }));
  }
  for (const [k, f] of Object.entries(files)) {
    if (f) fd.append(k, f, f.name);
  }
  return fd;
}
