import { Link } from "wouter";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ThemeToggle } from "@/components/theme-toggle";
import { ANDROID_APK } from "@/lib/android-download";
import { motion } from "framer-motion";
import {
  Shield,
  Download,
  Smartphone,
  CheckCircle2,
  ExternalLink,
  ArrowLeft,
  QrCode,
  Fingerprint,
  Lock,
  Zap,
} from "lucide-react";
import { SiStellar } from "react-icons/si";

const fadeUp = {
  initial: { opacity: 0, y: 20 },
  animate: { opacity: 1, y: 0 },
  transition: { duration: 0.5, ease: [0.16, 1, 0.3, 1] },
};

const features = [
  {
    icon: QrCode,
    title: "Scan & Prove",
    body: "Scan verifier QRs and present credentials in one flow.",
  },
  {
    icon: Fingerprint,
    title: "ZK share",
    body: "Generate sigma-protocol proofs and share a verify link.",
  },
  {
    icon: Lock,
    title: "Wallet session",
    body: "Connect Freighter / EVM and keep holder credentials synced.",
  },
  {
    icon: Zap,
    title: "Offline-friendly cache",
    body: "Cached credentials stay available when the API is slow.",
  },
];

export default function DownloadAppPage() {
  return (
    <div className="min-h-[100dvh] max-w-[100vw] overflow-x-hidden bg-background text-foreground relative stellar-space-bg grid-bg-overlay">
      <div className="absolute top-[12%] left-[8%] w-72 h-72 rounded-full bg-primary/10 blur-[100px] pointer-events-none" />
      <div className="absolute bottom-[10%] right-[5%] w-96 h-96 rounded-full bg-sky-500/10 blur-[120px] pointer-events-none" />

      <header className="sticky top-0 z-40 border-b border-white/5 bg-background/55 backdrop-blur-xl">
        <div className="max-w-5xl mx-auto flex items-center justify-between gap-3 px-4 sm:px-6 py-3">
          <Link
            href="/"
            className="flex items-center gap-2 min-w-0 rounded-md outline-none hover:opacity-90"
          >
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 border border-primary/20">
              <Shield className="h-4 w-4 text-primary" />
            </div>
            <span className="font-serif font-bold text-lg truncate">
              Krydo
              <span className="text-primary font-sans font-medium text-xs align-super ml-0.5">
                android
              </span>
            </span>
          </Link>
          <div className="flex items-center gap-2">
            <ThemeToggle />
            <Button asChild variant="ghost" size="sm" className="rounded-full hidden sm:inline-flex">
              <Link href="/">
                <ArrowLeft className="w-4 h-4 mr-1.5" />
                Home
              </Link>
            </Button>
          </div>
        </div>
      </header>

      <main className="relative z-10 max-w-5xl mx-auto px-4 sm:px-6 py-10 sm:py-16">
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-10 items-start">
          <motion.div
            className="lg:col-span-7 space-y-6"
            initial="initial"
            animate="animate"
            variants={{ animate: { transition: { staggerChildren: 0.08 } } }}
          >
            <motion.div variants={fadeUp}>
              <Badge
                variant="secondary"
                className="rounded-full border border-primary/20 bg-primary/10 text-primary px-3 py-1 text-xs font-semibold"
              >
                <Smartphone className="w-3 h-3 mr-1.5" />
                Android APK · {ANDROID_APK.versionName}
              </Badge>
            </motion.div>

            <motion.h1
              variants={fadeUp}
              className="font-serif text-4xl sm:text-5xl font-bold tracking-tight leading-[1.1]"
            >
              Download the{" "}
              <span className="gradient-text-stellar">Krydo</span> Android app
            </motion.h1>

            <motion.p
              variants={fadeUp}
              className="text-base sm:text-lg text-muted-foreground leading-relaxed max-w-xl"
            >
              Hold credentials, scan presentation QRs, and share ZK proofs from your phone.
              Sideload the APK below — Play Store listing coming later.
            </motion.p>

            <motion.div variants={fadeUp} className="flex flex-wrap gap-3">
              <Button
                size="lg"
                className="rounded-full px-7 h-12 text-base shadow-lg shadow-primary/25"
                asChild
              >
                <a
                  href={ANDROID_APK.downloadUrl}
                  download={ANDROID_APK.fileName}
                  data-testid="button-download-apk"
                >
                  <Download className="w-5 h-5 mr-2" />
                  Download APK
                </a>
              </Button>
              <Button
                size="lg"
                variant="outline"
                className="rounded-full px-6 h-12 border-white/10 bg-white/5"
                asChild
              >
                <a
                  href={ANDROID_APK.releasePageUrl}
                  target="_blank"
                  rel="noreferrer"
                  data-testid="link-release-notes"
                >
                  Release notes
                  <ExternalLink className="w-4 h-4 ml-2" />
                </a>
              </Button>
            </motion.div>

            <motion.ul
              variants={fadeUp}
              className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-2"
            >
              {[
                `${ANDROID_APK.sizeLabel} download`,
                ANDROID_APK.minAndroid,
                `Package ${ANDROID_APK.packageId}`,
                "Direct from GitHub Releases",
              ].map((item) => (
                <li
                  key={item}
                  className="flex items-center gap-2 text-sm text-muted-foreground"
                >
                  <CheckCircle2 className="w-4 h-4 text-primary shrink-0" />
                  {item}
                </li>
              ))}
            </motion.ul>

            <motion.div
              variants={fadeUp}
              className="rounded-2xl border border-white/10 bg-card/40 backdrop-blur-xl p-4 sm:p-5 space-y-3"
            >
              <p className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
                Install steps
              </p>
              <ol className="space-y-2 text-sm text-muted-foreground list-decimal list-inside">
                <li>Download the APK on your Android phone.</li>
                <li>Allow install from this browser / Files (Unknown sources).</li>
                <li>Open the APK → Install → Open Krydo.</li>
                <li>Connect wallet, then Request → Prove → ZK share.</li>
              </ol>
            </motion.div>
          </motion.div>

          <motion.div
            className="lg:col-span-5"
            initial={{ opacity: 0, y: 24 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.55, delay: 0.15 }}
          >
            <div className="relative rounded-3xl border border-white/15 bg-white/10 dark:bg-slate-950/40 backdrop-blur-3xl p-5 sm:p-6 shadow-2xl overflow-hidden">
              <div className="absolute -top-10 -right-10 w-40 h-40 rounded-full bg-primary/20 blur-3xl pointer-events-none" />
              <div className="flex items-center justify-between mb-5 relative">
                <div className="flex items-center gap-2">
                  <SiStellar className="w-5 h-5 text-primary" />
                  <span className="font-serif font-bold">Krydo Mobile</span>
                </div>
                <Badge variant="outline" className="rounded-full text-[10px] font-mono">
                  v{ANDROID_APK.versionName}
                </Badge>
              </div>

              <div className="rounded-2xl border border-primary/25 bg-gradient-to-br from-primary/20 via-sky-500/10 to-transparent p-6 mb-5 text-center">
                <div className="mx-auto mb-3 flex h-16 w-16 items-center justify-center rounded-2xl bg-primary/20 border border-primary/30">
                  <Smartphone className="h-8 w-8 text-primary" />
                </div>
                <p className="font-serif text-xl font-bold">Ready to install</p>
                <p className="text-xs text-muted-foreground mt-1">
                  {ANDROID_APK.fileName}
                </p>
                <Button className="mt-4 w-full rounded-full" asChild>
                  <a href={ANDROID_APK.downloadUrl} download={ANDROID_APK.fileName}>
                    <Download className="w-4 h-4 mr-2" />
                    Get APK
                  </a>
                </Button>
              </div>

              <div className="space-y-3 relative">
                {features.map(({ icon: Icon, title, body }) => (
                  <div
                    key={title}
                    className="flex gap-3 rounded-xl border border-white/10 bg-background/30 p-3"
                  >
                    <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-primary/10 border border-primary/20">
                      <Icon className="h-4 w-4 text-primary" />
                    </div>
                    <div>
                      <p className="text-sm font-semibold text-foreground">{title}</p>
                      <p className="text-xs text-muted-foreground leading-relaxed">{body}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </motion.div>
        </div>
      </main>
    </div>
  );
}
