import { useEffect } from "react";

const SITE_URL = "https://www.krydo.in";
const DEFAULT_TITLE = "Krydo — Privacy-Preserving Financial Trust on Stellar";
const DEFAULT_DESCRIPTION =
  "Krydo (krydo.in) is a Stellar Soroban product for verifiable credentials and zero-knowledge proofs — prove income or credit score without revealing the raw number. Unrelated to Kreedo education.";
const DEFAULT_IMAGE = `${SITE_URL}/og-image.png`;
const DISAMBIGUATION =
  "Krydo is a Stellar blockchain verifiable-credentials and ZK-proofs product at krydo.in. It is not Kreedo Early Childhood Solutions.";
const OG_IMAGE_WIDTH = "1200";
const OG_IMAGE_HEIGHT = "630";

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
    config.noIndex
      ? "noindex, nofollow"
      : "index, follow, max-image-preview:large, max-snippet:-1, max-video-preview:-1",
  );
  upsertMeta("name", "googlebot", config.noIndex ? "noindex, nofollow" : "index, follow, max-image-preview:large");

  upsertLink("canonical", url);

  upsertMeta("property", "og:site_name", "Krydo");
  upsertMeta("property", "og:locale", "en_IN");
  upsertMeta("property", "og:type", type);
  upsertMeta("property", "og:title", title);
  upsertMeta("property", "og:description", description);
  upsertMeta("property", "og:url", url);
  upsertMeta("property", "og:image", image);
  upsertMeta("property", "og:image:type", image.endsWith(".svg") ? "image/svg+xml" : "image/png");
  upsertMeta("property", "og:image:width", OG_IMAGE_WIDTH);
  upsertMeta("property", "og:image:height", OG_IMAGE_HEIGHT);
  upsertMeta("property", "og:image:alt", "Krydo — privacy-preserving financial trust on Stellar");

  upsertMeta("name", "twitter:card", "summary_large_image");
  upsertMeta("name", "twitter:title", title);
  upsertMeta("name", "twitter:description", description);
  upsertMeta("name", "twitter:image", image);
  upsertMeta("name", "twitter:image:alt", "Krydo — privacy-preserving financial trust on Stellar");

  if (!config.noIndex && (path === "/" || path === "" || path === "/about")) {
    upsertJsonLd("krydo-jsonld-org", {
      "@context": "https://schema.org",
      "@type": "Organization",
      "@id": `${SITE_URL}/#organization`,
      name: "Krydo",
      legalName: "Krydo",
      alternateName: ["Krydo SSI", "Krydo Stellar", "Krydo Next"],
      url: SITE_URL,
      logo: {
        "@type": "ImageObject",
        url: `${SITE_URL}/logo.png`,
        width: 512,
        height: 512,
      },
      description: DEFAULT_DESCRIPTION,
      disambiguatingDescription: DISAMBIGUATION,
      foundingDate: "2026",
      founder: [
        {
          "@type": "Person",
          "@id": `${SITE_URL}/#nishant`,
          name: "Nishant Agarwal",
          url: "https://nishantx.in",
          jobTitle: "Co-founder",
          sameAs: [
            "https://github.com/nishant-uxs",
            "https://nishantx.in",
            "https://dev.to/agarwalnishant812",
          ],
        },
        {
          "@type": "Person",
          "@id": `${SITE_URL}/#adarsh`,
          name: "Adarsh Agrahari",
          jobTitle: "Co-founder",
        },
      ],
      sameAs: [
        "https://github.com/nishant-uxs/krydo-next",
        "https://github.com/nishant-uxs/krydo-stellar",
        "https://nishantx.in",
        "https://dev.to/agarwalnishant812",
        "https://dev.to/agarwalnishant812/what-is-krydo-and-why-google-keeps-saying-kreedo-kk1",
        "https://github.com/nishant-uxs",
      ],
      knowsAbout: [
        "Stellar",
        "Soroban",
        "verifiable credentials",
        "zero-knowledge proofs",
        "self-sovereign identity",
      ],
    });
    upsertJsonLd("krydo-jsonld-website", {
      "@context": "https://schema.org",
      "@type": "WebSite",
      "@id": `${SITE_URL}/#website`,
      name: "Krydo",
      alternateName: "krydo.in",
      url: SITE_URL,
      description: DEFAULT_DESCRIPTION,
      publisher: { "@id": `${SITE_URL}/#organization` },
      inLanguage: "en-IN",
    });
    upsertJsonLd("krydo-jsonld-software", {
      "@context": "https://schema.org",
      "@type": "SoftwareApplication",
      name: "Krydo",
      alternateName: "Krydo on Stellar",
      applicationCategory: "FinanceApplication",
      operatingSystem: "Web, Android",
      url: SITE_URL,
      description:
        "Issue and verify privacy-preserving credentials on Stellar using Soroban contracts and sigma-protocol ZK proofs.",
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

  if (path === "/about") {
    return {
      path: "/about",
      title: "What is Krydo? Founders & Brand | Stellar Credentials (not Kreedo)",
      description:
        "Krydo (krydo.in) is a Stellar Soroban verifiable-credentials and ZK-proofs product founded by Nishant Agarwal and Adarsh Agrahari. Unrelated to Kreedo Early Childhood Solutions.",
      keywords:
        "What is Krydo, founder of Krydo, Nishant Agarwal, Adarsh Agrahari, Krydo vs Kreedo, Stellar SSI, krydo.in",
    };
  }

  if (path === "/download" || path === "/app") {
    return {
      path: "/download",
      title: "Download Krydo Android APK | ZK Credentials on Stellar",
      description:
        "Download the Krydo Android APK — hold Stellar-anchored credentials, scan verifier QRs, and share zero-knowledge proofs without revealing raw claim data.",
      keywords:
        "Krydo APK, Android download, verifiable credentials, ZK proofs, Stellar, Freighter, SIWS",
    };
  }

  // Public verifier landing — indexable.
  if (path === "/verify") {
    return {
      path: "/verify",
      title: "Verify Credentials & ZK Proofs | Krydo Public Verifier",
      description:
        "Open a Krydo proof link and check the on-chain credential anchor plus zero-knowledge validity — without seeing plaintext claim values. Built on Stellar Soroban.",
      keywords:
        "verify credential, ZK proof verifier, zero knowledge, Stellar Soroban, Krydo, SIWS",
    };
  }

  // Per-proof URLs are thin / ephemeral — keep them out of the index.
  if (path.startsWith("/verify/")) {
    return {
      path,
      title: "Verify Proof | Krydo",
      description: DEFAULT_DESCRIPTION,
      noIndex: true,
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
      "Krydo, krydo.in, founder of Krydo, Nishant Agarwal, Stellar, Soroban, verifiable credentials, zero knowledge proofs, Krydo vs Kreedo, SSI India",
  };
}

export { SITE_URL, DEFAULT_TITLE, DEFAULT_DESCRIPTION };
