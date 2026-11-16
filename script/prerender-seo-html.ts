/**
 * After Vite build, write one HTML shell per public route with the correct
 * <title>, description, and canonical so Google does not treat /about etc.
 * as "Alternate page with proper canonical tag" pointing at /.
 */
import { mkdir, readFile, writeFile } from "fs/promises";
import path from "path";

const SITE = "https://www.krydo.in";
const OUT = path.resolve("dist/public");

type Page = {
  /** URL path, e.g. /about */
  route: string;
  /** Output folder under dist/public ("" = index.html root) */
  dir: string;
  title: string;
  description: string;
  canonicalPath: string;
};

const PAGES: Page[] = [
  {
    route: "/",
    dir: "",
    title: "Krydo — Privacy-Preserving Financial Trust on Stellar",
    description:
      "Krydo (krydo.in) — Stellar Soroban verifiable credentials and zero-knowledge proofs. Prove income or credit score without revealing the raw number. Not related to Kreedo education.",
    canonicalPath: "/",
  },
  {
    route: "/about",
    dir: "about",
    title: "What is Krydo? Founders & Brand | Stellar Credentials (not Kreedo)",
    description:
      "Krydo (krydo.in) is a Stellar Soroban verifiable-credentials and ZK-proofs product founded by Nishant Agarwal and Adarsh Agrahari. Unrelated to Kreedo Early Childhood Solutions.",
    canonicalPath: "/about",
  },
  {
    route: "/download",
    dir: "download",
    title: "Download Krydo Android APK | ZK Credentials on Stellar",
    description:
      "Download the Krydo Android APK — hold Stellar-anchored credentials, scan verifier QRs, and share zero-knowledge proofs without revealing raw claim data.",
    canonicalPath: "/download",
  },
  {
    route: "/app",
    dir: "app",
    title: "Download Krydo Android APK | ZK Credentials on Stellar",
    description:
      "Download the Krydo Android APK — hold Stellar-anchored credentials, scan verifier QRs, and share zero-knowledge proofs without revealing raw claim data.",
    canonicalPath: "/download",
  },
  {
    route: "/verify",
    dir: "verify",
    title: "Verify Credentials & ZK Proofs | Krydo Public Verifier",
    description:
      "Open a Krydo proof link and check the on-chain credential anchor plus zero-knowledge validity — without seeing plaintext claim values. Built on Stellar Soroban.",
    canonicalPath: "/verify",
  },
];

function escapeAttr(s: string): string {
  return s
    .replace(/&/g, "&amp;")
    .replace(/"/g, "&quot;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");
}

function applyHead(html: string, page: Page): string {
  const canonical = `${SITE}${page.canonicalPath === "/" ? "/" : page.canonicalPath}`;
  const title = escapeAttr(page.title);
  const description = escapeAttr(page.description);
  const url = escapeAttr(canonical);

  let out = html;
  out = out.replace(/<title>[^<]*<\/title>/i, `<title>${title}</title>`);
  out = out.replace(
    /<meta\s+name="description"\s+content="[^"]*"\s*\/?>/i,
    `<meta name="description" content="${description}" />`,
  );
  out = out.replace(
    /<link\s+rel="canonical"\s+href="[^"]*"\s*\/?>/i,
    `<link rel="canonical" href="${url}" />`,
  );
  out = out.replace(
    /<meta\s+property="og:url"\s+content="[^"]*"\s*\/?>/i,
    `<meta property="og:url" content="${url}" />`,
  );
  out = out.replace(
    /<meta\s+property="og:title"\s+content="[^"]*"\s*\/?>/i,
    `<meta property="og:title" content="${title}" />`,
  );
  out = out.replace(
    /<meta\s+property="og:description"\s+content="[^"]*"\s*\/?>/i,
    `<meta property="og:description" content="${description}" />`,
  );
  out = out.replace(
    /<meta\s+name="twitter:title"\s+content="[^"]*"\s*\/?>/i,
    `<meta name="twitter:title" content="${title}" />`,
  );
  out = out.replace(
    /<meta\s+name="twitter:description"\s+content="[^"]*"\s*\/?>/i,
    `<meta name="twitter:description" content="${description}" />`,
  );
  return out;
}

export async function prerenderSeoHtml() {
  const template = await readFile(path.join(OUT, "index.html"), "utf8");

  for (const page of PAGES) {
    const html = applyHead(template, page);
    if (!page.dir) {
      await writeFile(path.join(OUT, "index.html"), html, "utf8");
      console.log(`seo: wrote index.html → ${page.canonicalPath}`);
      continue;
    }
    const dir = path.join(OUT, page.dir);
    await mkdir(dir, { recursive: true });
    await writeFile(path.join(dir, "index.html"), html, "utf8");
    console.log(`seo: wrote ${page.dir}/index.html → ${page.canonicalPath}`);
  }
}

