import { Link } from "wouter";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { ThemeToggle } from "@/components/theme-toggle";
import { motion } from "framer-motion";
import { Shield, ArrowLeft, ExternalLink, Github } from "lucide-react";
import { SiStellar } from "react-icons/si";

const fadeUp = {
  initial: { opacity: 0, y: 20 },
  animate: { opacity: 1, y: 0 },
  transition: { duration: 0.5, ease: [0.16, 1, 0.3, 1] as const },
};

export default function AboutPage() {
  return (
    <div className="min-h-[100dvh] max-w-[100vw] overflow-x-hidden bg-background text-foreground relative stellar-space-bg grid-bg-overlay">
      <div className="absolute top-[12%] left-[8%] w-72 h-72 rounded-full bg-primary/10 blur-[100px] pointer-events-none" />
      <div className="absolute bottom-[10%] right-[5%] w-96 h-96 rounded-full bg-sky-500/10 blur-[120px] pointer-events-none" />

      <header className="sticky top-0 z-40 border-b border-white/5 bg-background/55 backdrop-blur-xl">
        <div className="max-w-3xl mx-auto flex items-center justify-between gap-3 px-4 sm:px-6 py-3">
          <Link
            href="/"
            className="flex items-center gap-2 min-w-0 rounded-md outline-none hover:opacity-90"
          >
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary/10 border border-primary/20">
              <Shield className="h-4 w-4 text-primary" />
            </div>
            <span className="font-serif font-bold text-lg truncate">Krydo</span>
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

      <main className="relative z-10 max-w-3xl mx-auto px-4 sm:px-6 py-12 sm:py-16 space-y-12">
        <motion.section initial="initial" animate="animate" variants={fadeUp} className="space-y-4">
          <Badge
            variant="secondary"
            className="py-1.5 px-3 bg-primary/10 text-primary border border-primary/20 rounded-full font-serif text-xs"
          >
            <SiStellar className="w-3 h-3 mr-1.5 inline" />
            About the brand
          </Badge>
          <h1 className="font-serif text-4xl sm:text-5xl font-bold tracking-tight">
            What is Krydo?
          </h1>
          <p className="text-lg text-muted-foreground leading-relaxed">
            <strong className="text-foreground">Krydo</strong> (domain{" "}
            <a className="text-primary underline-offset-4 hover:underline" href="https://www.krydo.in">
              krydo.in
            </a>
            ) is privacy-preserving financial trust infrastructure on the{" "}
            <strong className="text-foreground">Stellar</strong> network. Issuers write credential
            hashes to Soroban smart contracts. Holders prove predicates with zero-knowledge proofs —
            for example credit score ≥ 700 — without revealing the raw number.
          </p>
        </motion.section>

        <motion.section
          initial="initial"
          animate="animate"
          variants={fadeUp}
          className="rounded-2xl border border-white/10 bg-white/5 p-6 sm:p-8 space-y-3"
        >
          <h2 className="font-serif text-2xl font-bold">Krydo is not Kreedo</h2>
          <p className="text-muted-foreground leading-relaxed">
            Krydo is a Web3 / Stellar credentials product. It is{" "}
            <strong className="text-foreground">not</strong> related to Kreedo Early Childhood
            Solutions (an education company). If you searched for the Stellar credentials project,
            you want <strong className="text-foreground">Krydo</strong> at krydo.in.
          </p>
        </motion.section>

        <motion.section initial="initial" animate="animate" variants={fadeUp} className="space-y-4">
          <h2 className="font-serif text-2xl font-bold">Who founded Krydo?</h2>
          <p className="text-muted-foreground leading-relaxed">
            Krydo was built by students at Bennett University. Lead builders:
          </p>
          <ul className="space-y-4">
            <li className="rounded-xl border border-white/10 bg-background/40 p-4">
              <p className="font-semibold text-foreground">Nishant Agarwal</p>
              <p className="text-sm text-muted-foreground mt-1">
                Co-founder / engineer — architecture, cryptography, Stellar / Soroban integration.
              </p>
              <div className="mt-2 flex flex-wrap gap-3 text-sm">
                <a
                  className="text-primary inline-flex items-center gap-1 hover:underline"
                  href="https://nishantx.in"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  nishantx.in <ExternalLink className="w-3 h-3" />
                </a>
                <a
                  className="text-primary inline-flex items-center gap-1 hover:underline"
                  href="https://github.com/nishant-uxs"
                  target="_blank"
                  rel="noopener noreferrer"
                >
                  <Github className="w-3.5 h-3.5" /> nishant-uxs
                </a>
              </div>
            </li>
            <li className="rounded-xl border border-white/10 bg-background/40 p-4">
              <p className="font-semibold text-foreground">Adarsh Agrahari</p>
              <p className="text-sm text-muted-foreground mt-1">
                Co-founder / engineer — product and full-stack delivery on Krydo.
              </p>
            </li>
          </ul>
          <p className="text-sm text-muted-foreground">
            Krydo earned an Honorable Mention at Stellar Buildathon Delhi for its privacy credential
            protocol on three Soroban contracts.
          </p>
        </motion.section>

        <motion.section initial="initial" animate="animate" variants={fadeUp} className="space-y-3">
          <h2 className="font-serif text-2xl font-bold">Stack at a glance</h2>
          <ul className="list-disc pl-5 text-muted-foreground space-y-2">
            <li>Stellar Testnet · Soroban contracts (Authority, Credentials, Audit)</li>
            <li>Sign-In with Stellar (SIWS / SEP-53) · Freighter and Stellar Wallets Kit</li>
            <li>Sigma-protocol ZK proofs over Pedersen commitments</li>
            <li>Web app at krydo.in · Android APK · public verifier at /verify</li>
          </ul>
        </motion.section>

        <motion.div
          initial="initial"
          animate="animate"
          variants={fadeUp}
          className="flex flex-wrap gap-3 pt-2"
        >
          <Button asChild className="rounded-full">
            <Link href="/">Open Krydo</Link>
          </Button>
          <Button asChild variant="outline" className="rounded-full">
            <Link href="/download">Download APK</Link>
          </Button>
          <Button asChild variant="ghost" className="rounded-full">
            <a
              href="https://github.com/nishant-uxs/krydo-next"
              target="_blank"
              rel="noopener noreferrer"
            >
              <Github className="w-4 h-4 mr-1.5" />
              GitHub
            </a>
          </Button>
        </motion.div>
      </main>

      <footer className="relative z-10 border-t border-white/5 py-8 text-center text-xs text-muted-foreground">
        © 2026 Krydo · Built on Stellar Soroban ·{" "}
        <a href="https://www.krydo.in" className="text-primary hover:underline">
          www.krydo.in
        </a>
      </footer>
    </div>
  );
}
