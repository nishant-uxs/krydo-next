import { useEffect } from "react";

const SITE_URL = "https://www.krydo.in";
const DEFAULT_TITLE = "Krydo — Privacy-Preserving Financial Trust on Stellar";
const DEFAULT_DESCRIPTION =
  "Prove financial credibility with verifiable credentials and zero-knowledge proofs — without revealing sensitive data. Built on Stellar Soroban.";
const DEFAULT_IMAGE = `${SITE_URL}/og-image.svg`;

export type SeoConfig = {
  title?: string;
  description?: string;
  path?: string;
  image?: string;
  type?: "website" | "article";
  noIndex?: boolean;
  keywords?: string;
};

function upsertMeta(attr: "name" | "property", key: string, content: string) {
  let el = document.head.querySelector<HTMLMetaElement>(`meta[${attr}="${key}"]`);
  if (!el) {
    el = document.createElement("meta");
    el.setAttribute(attr, key);
    document.head.appendChild(el);
  }
  el.setAttribute("content", content);
}

function upsertLink(rel: string, href: string) {
  let el = document.head.querySelector<HTMLLinkElement>(`link[rel="${rel}"]`);
  if (!el) {
    el = document.createElement("link");
    el.setAttribute("rel", rel);
    document.head.appendChild(el);
  }
  el.setAttribute("href", href);
}

function upsertJsonLd(id: string, data: Record<string, unknown>) {
  let el = document.getElementById(id) as HTMLScriptElement | null;
  if (!el) {
    el = document.createElement("script");
    el.type = "application/ld+json";
    el.id = id;
    document.head.appendChild(el);
  }
  el.textContent = JSON.stringify(data);
}

/** Route-aware document head for SPA SEO (title, description, OG, Twitter, robots). */
export function applySeo(config: SeoConfig = {}) {
  const title = config.title?.trim() || DEFAULT_TITLE;
  const description = config.description?.trim() || DEFAULT_DESCRIPTION;
  const path = config.path?.startsWith("/") ? config.path : `/${config.path || ""}`;
  const url = `${SITE_URL}${path === "/" ? "" : path}`;
  const image = config.image || DEFAULT_IMAGE;
  const type = config.type || "website";

  document.title = title;

  upsertMeta("name", "description", description);
  if (config.keywords) upsertMeta("name", "keywords", config.keywords);
  upsertMeta(
    "name",
    "robots",
    config.noIndex ? "noindex, nofollow" : "index, follow, max-image-preview:large",
  );
  upsertMeta("name", "googlebot", config.noIndex ? "noindex, nofollow" : "index, follow");

  upsertLink("canonical", url);

  upsertMeta("property", "og:site_name", "Krydo");
  upsertMeta("property", "og:locale", "en_IN");
  upsertMeta("property", "og:type", type);
  upsertMeta("property", "og:title", title);
  upsertMeta("property", "og:description", description);
  upsertMeta("property", "og:url", url);
  upsertMeta("property", "og:image", image);
  upsertMeta("property", "og:image:alt", "Krydo — privacy-preserving financial trust");

  upsertMeta("name", "twitter:card", "summary_large_image");
  upsertMeta("name", "twitter:title", title);
  upsertMeta("name", "twitter:description", description);
  upsertMeta("name", "twitter:image", image);

  if (!config.noIndex && (path === "/" || path === "")) {
    upsertJsonLd("krydo-jsonld-org", {
      "@context": "https://schema.org",
      "@type": "Organization",
      name: "Krydo",
      url: SITE_URL,
      logo: `${SITE_URL}/favicon.svg`,
      description: DEFAULT_DESCRIPTION,
      sameAs: [
        "https://github.com/nishant-uxs/krydo-next",
      ],
    });
    upsertJsonLd("krydo-jsonld-website", {
      "@context": "https://schema.org",
      "@type": "WebSite",
      name: "Krydo",
      url: SITE_URL,
      description: DEFAULT_DESCRIPTION,
    });
    upsertJsonLd("krydo-jsonld-software", {
      "@context": "https://schema.org",
      "@type": "SoftwareApplication",
      name: "Krydo Android",
      operatingSystem: "Android",
      applicationCategory: "FinanceApplication",
      offers: {
        "@type": "Offer",
        price: "0",
        priceCurrency: "INR",
        url: `${SITE_URL}/download`,
      },
      downloadUrl:
        "https://github.com/nishant-uxs/krydo-next/releases/download/android-0.27.0/krydo-0.27.0.apk",
    });
  }
}

export function SeoHead(config: SeoConfig) {
  useEffect(() => {
    applySeo(config);
  }, [
    config.title,
    config.description,
    config.path,
    config.image,
    config.type,
    config.noIndex,
    config.keywords,
  ]);
  return null;
}

/** Map wouter location → SEO config for public + app routes. */
export function seoForPath(pathname: string): SeoConfig {
  const path = pathname.split("?")[0] || "/";

  if (path === "/download" || path === "/app") {
    return {
      path: "/download",
      title: "Download Krydo Android APK | Privacy-Preserving Credentials",
      description:
        "Download the Krydo Android APK to hold credentials, scan verifier QRs, and share zero-knowledge proofs on Stellar.",
      keywords:
        "Krydo APK, Android download, verifiable credentials, ZK proofs, Stellar wallet app",
    };
  }

  if (path === "/verify" || path.startsWith("/verify/")) {
    return {
      path: path.startsWith("/verify/") ? "/verify" : path,
      title: "Verify Credentials & ZK Proofs | Krydo Public Verifier",
      description:
        "Public Krydo verifier — check credential anchors and zero-knowledge proof validity without seeing plaintext claim values.",
      keywords:
        "verify credential, ZK proof verifier, zero knowledge, Stellar Soroban, Krydo",
    };
  }

  // Authenticated product surfaces — keep out of search indexes.
  const privatePrefixes = [
    "/dashboard",
    "/issuers",
    "/issue",
    "/request",
    "/credentials",
    "/zk-proofs",
    "/transactions",
  ];
  if (privatePrefixes.some((p) => path === p || path.startsWith(`${p}/`))) {
    return {
      path,
      title: "Krydo App",
      description: DEFAULT_DESCRIPTION,
      noIndex: true,
    };
  }

  return {
    path: "/",
    title: DEFAULT_TITLE,
    description: DEFAULT_DESCRIPTION,
    keywords:
      "Krydo, zero knowledge proofs, verifiable credentials, Stellar, Soroban, financial privacy, SSI, ZK credentials India",
  };
}

export { SITE_URL, DEFAULT_TITLE, DEFAULT_DESCRIPTION };
