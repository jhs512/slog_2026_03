const NEXT_PUBLIC_API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL;

export async function fetchOneTimeToken(
  allowedPathPrefix: string,
): Promise<string | null> {
  try {
    const res = await fetch(
      `${NEXT_PUBLIC_API_BASE_URL}/member/api/v1/auth/oneTimeToken`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({ allowedPathPrefix }),
      },
    );
    if (!res.ok) return null;
    const json = await res.json();
    return json.data?.oneTimeToken ?? null;
  } catch {
    return null;
  }
}
