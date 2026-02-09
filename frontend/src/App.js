import React, { useEffect, useMemo, useState } from "react";
import { Toaster, toast } from "sonner";
import {
    ArrowDownLeft,
    ArrowUpRight,
    Copy,
    Loader2,
    Plus,
    RefreshCw,
    Server,
    Wallet as WalletIcon,
} from "lucide-react";
import "./App.css";
import { walletApi, extractError } from "./lib/walletApi";
import { Button } from "./components/ui/button";
import { Input } from "./components/ui/input";
import { Label } from "./components/ui/label";
import {
    Tabs,
    TabsContent,
    TabsList,
    TabsTrigger,
} from "./components/ui/tabs";

function formatAmount(value) {
    if (value === null || value === undefined) return "—";
    const n = Number(value);
    if (Number.isNaN(n)) return String(value);
    return n.toLocaleString("en-US", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
    });
}

function shortUuid(id) {
    if (!id) return "";
    return `${id.slice(0, 8)}…${id.slice(-4)}`;
}

function TopBar() {
    return (
        <header
            className="relative z-10 flex items-center justify-between px-6 md:px-10 py-5 border-b"
            style={{ borderColor: "var(--border-soft)" }}
            data-testid="app-topbar"
        >
            <div className="flex items-center gap-3">
                <div
                    className="h-9 w-9 rounded-md flex items-center justify-center"
                    style={{ background: "var(--accent)", color: "var(--accent-ink)" }}
                >
                    <WalletIcon size={18} strokeWidth={2.25} />
                </div>
                <div className="leading-tight">
                    <div className="font-serif text-2xl" data-testid="app-title">
                        Vallet
                    </div>
                    <div
                        className="text-xs font-mono"
                        style={{ color: "var(--text-muted)" }}
                    >
                        high-concurrency wallet ledger
                    </div>
                </div>
            </div>
            <div
                className="hidden sm:flex items-center gap-2 font-mono text-xs px-3 py-1.5 rounded-full"
                style={{
                    background: "var(--bg-elev)",
                    border: "1px solid var(--border-soft)",
                    color: "var(--text-muted)",
                }}
                data-testid="api-base-url"
                title={walletApi.baseUrl}
            >
                <Server size={12} />
                <span>{walletApi.baseUrl}</span>
            </div>
        </header>
    );
}

function WalletHeroCard({ wallet, loading, onRefresh }) {
    return (
        <section
            className="relative z-10 rounded-2xl overflow-hidden animate-fade-up"
            style={{
                background:
                    "linear-gradient(145deg, var(--bg-elev-2) 0%, var(--bg-elev) 100%)",
                border: "1px solid var(--border-soft)",
            }}
            data-testid="wallet-hero"
        >
            <div className="p-8 md:p-12">
                <div
                    className="text-xs font-mono uppercase tracking-[0.22em]"
                    style={{ color: "var(--text-muted)" }}
                >
                    Current balance
                </div>
                <div className="mt-4 flex items-baseline gap-3">
                    <span
                        className="font-serif text-6xl md:text-7xl"
                        data-testid="wallet-balance"
                    >
                        {wallet ? formatAmount(wallet.balance) : "—"}
                    </span>
                    <span
                        className="font-mono text-sm"
                        style={{ color: "var(--text-muted)" }}
                    >
                        units
                    </span>
                </div>

                <div className="mt-6 flex flex-wrap items-center gap-3">
                    {wallet ? (
                        <>
                            <span
                                className="font-mono text-xs px-2.5 py-1 rounded-md"
                                style={{
                                    background: "var(--bg-elev-2)",
                                    border: "1px solid var(--border-soft)",
                                    color: "var(--text-muted)",
                                }}
                                data-testid="wallet-id-badge"
                            >
                                {shortUuid(wallet.walletId)}
                            </span>
                            <button
                                onClick={() => {
                                    navigator.clipboard.writeText(wallet.walletId);
                                    toast.success("Wallet ID copied");
                                }}
                                className="inline-flex items-center gap-1.5 text-xs font-mono hover:opacity-80 transition-opacity"
                                style={{ color: "var(--text-muted)" }}
                                data-testid="copy-wallet-id-btn"
                            >
                                <Copy size={12} />
                                copy full id
                            </button>
                        </>
                    ) : (
                        <span
                            className="text-sm"
                            style={{ color: "var(--text-muted)" }}
                        >
                            Select or create a wallet to see its balance.
                        </span>
                    )}
                    {wallet && (
                        <button
                            onClick={onRefresh}
                            className="ml-auto inline-flex items-center gap-1.5 text-xs font-mono hover:opacity-80 transition-opacity"
                            style={{ color: "var(--accent)" }}
                            disabled={loading}
                            data-testid="refresh-balance-btn"
                        >
                            {loading ? (
                                <Loader2 size={12} className="animate-spin" />
                            ) : (
                                <RefreshCw size={12} />
                            )}
                            refresh
                        </button>
                    )}
                </div>
            </div>
        </section>
    );
}

function WalletPicker({ onPick, onCreated }) {
    const [walletId, setWalletId] = useState("");
    const [initialBalance, setInitialBalance] = useState("");
    const [loading, setLoading] = useState(false);
    const [creating, setCreating] = useState(false);

    const handleLoad = async (e) => {
        e?.preventDefault();
        if (!walletId) return;
        setLoading(true);
        try {
            const data = await walletApi.getBalance(walletId.trim());
            onPick(data);
            toast.success("Wallet loaded");
        } catch (err) {
            toast.error(extractError(err));
        } finally {
            setLoading(false);
        }
    };

    const handleCreate = async (e) => {
        e?.preventDefault();
        setCreating(true);
        try {
            const data = await walletApi.createWallet({
                initialBalance: initialBalance || 0,
            });
            onCreated(data);
            toast.success(`Wallet created — ${shortUuid(data.walletId)}`);
            setInitialBalance("");
        } catch (err) {
            toast.error(extractError(err));
        } finally {
            setCreating(false);
        }
    };

    return (
        <section
            className="relative z-10 rounded-2xl p-6 md:p-8 animate-fade-up"
            style={{
                background: "var(--bg-elev)",
                border: "1px solid var(--border-soft)",
            }}
            data-testid="wallet-picker"
        >
            <Tabs defaultValue="load" className="w-full">
                <TabsList
                    className="grid grid-cols-2 w-full mb-6"
                    style={{ background: "var(--bg-elev-2)" }}
                >
                    <TabsTrigger value="load" data-testid="tab-load-wallet">
                        Load wallet
                    </TabsTrigger>
                    <TabsTrigger value="create" data-testid="tab-create-wallet">
                        Create wallet
                    </TabsTrigger>
                </TabsList>

                <TabsContent value="load" className="space-y-4">
                    <form onSubmit={handleLoad} className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="walletId" className="font-mono text-xs uppercase tracking-wider">
                                Wallet UUID
                            </Label>
                            <Input
                                id="walletId"
                                placeholder="11111111-1111-1111-1111-111111111111"
                                value={walletId}
                                onChange={(e) => setWalletId(e.target.value)}
                                className="font-mono text-sm"
                                data-testid="wallet-id-input"
                                autoComplete="off"
                            />
                        </div>
                        <Button
                            type="submit"
                            disabled={!walletId || loading}
                            className="w-full gap-2"
                            style={{
                                background: "var(--accent)",
                                color: "var(--accent-ink)",
                            }}
                            data-testid="load-wallet-btn"
                        >
                            {loading && <Loader2 size={14} className="animate-spin" />}
                            Load balance
                        </Button>
                        <p
                            className="text-xs font-mono"
                            style={{ color: "var(--text-muted)" }}
                        >
                            Tip: the demo seed wallet is
                            <span style={{ color: "var(--text)" }}>
                                {" "}
                                11111111-1111-1111-1111-111111111111
                            </span>
                        </p>
                    </form>
                </TabsContent>

                <TabsContent value="create" className="space-y-4">
                    <form onSubmit={handleCreate} className="space-y-4">
                        <div className="space-y-2">
                            <Label htmlFor="initialBalance" className="font-mono text-xs uppercase tracking-wider">
                                Initial balance (optional)
                            </Label>
                            <Input
                                id="initialBalance"
                                type="number"
                                min="0"
                                step="0.01"
                                placeholder="0.00"
                                value={initialBalance}
                                onChange={(e) => setInitialBalance(e.target.value)}
                                className="font-mono text-sm"
                                data-testid="initial-balance-input"
                            />
                        </div>
                        <Button
                            type="submit"
                            disabled={creating}
                            className="w-full gap-2"
                            style={{
                                background: "var(--accent)",
                                color: "var(--accent-ink)",
                            }}
                            data-testid="create-wallet-btn"
                        >
                            {creating ? (
                                <Loader2 size={14} className="animate-spin" />
                            ) : (
                                <Plus size={14} />
                            )}
                            Generate new wallet
                        </Button>
                    </form>
                </TabsContent>
            </Tabs>
        </section>
    );
}

function OperationPanel({ wallet, onUpdated, onLog }) {
    const [amount, setAmount] = useState("");
    const [pending, setPending] = useState(null);
    const disabled = !wallet || pending !== null;

    const handleOp = async (operationType) => {
        if (!wallet || !amount) return;
        setPending(operationType);
        try {
            const data = await walletApi.operate({
                walletId: wallet.walletId,
                operationType,
                amount,
            });
            onUpdated(data);
            onLog({
                id: Date.now(),
                operationType,
                amount: Number(amount),
                balance: data.balance,
                at: new Date().toISOString(),
                status: "OK",
            });
            toast.success(`${operationType} ${formatAmount(amount)} ✓`);
            setAmount("");
        } catch (err) {
            const msg = extractError(err);
            onLog({
                id: Date.now(),
                operationType,
                amount: Number(amount),
                balance: wallet.balance,
                at: new Date().toISOString(),
                status: "FAIL",
                error: msg,
            });
            toast.error(msg);
        } finally {
            setPending(null);
        }
    };

    return (
        <section
            className="relative z-10 rounded-2xl p-6 md:p-8 animate-fade-up"
            style={{
                background: "var(--bg-elev)",
                border: "1px solid var(--border-soft)",
            }}
            data-testid="operation-panel"
        >
            <div className="flex items-center justify-between mb-6">
                <h2 className="font-serif text-2xl">Move money</h2>
                <span
                    className="text-xs font-mono"
                    style={{ color: "var(--text-muted)" }}
                >
                    POST /api/v1/wallet
                </span>
            </div>

            <div className="space-y-4">
                <div className="space-y-2">
                    <Label htmlFor="amount" className="font-mono text-xs uppercase tracking-wider">
                        Amount
                    </Label>
                    <Input
                        id="amount"
                        type="number"
                        min="0.01"
                        step="0.01"
                        placeholder="0.00"
                        value={amount}
                        onChange={(e) => setAmount(e.target.value)}
                        disabled={!wallet}
                        className="font-mono text-lg h-12"
                        data-testid="amount-input"
                    />
                </div>

                <div className="grid grid-cols-2 gap-3">
                    <Button
                        variant="outline"
                        onClick={() => handleOp("DEPOSIT")}
                        disabled={disabled || !amount}
                        className="h-12 gap-2"
                        style={{
                            background: "var(--bg-elev-2)",
                            borderColor: "var(--border-strong)",
                            color: "var(--text)",
                        }}
                        data-testid="deposit-btn"
                    >
                        {pending === "DEPOSIT" ? (
                            <Loader2 size={16} className="animate-spin" />
                        ) : (
                            <ArrowDownLeft size={16} style={{ color: "var(--accent)" }} />
                        )}
                        Deposit
                    </Button>
                    <Button
                        variant="outline"
                        onClick={() => handleOp("WITHDRAW")}
                        disabled={disabled || !amount}
                        className="h-12 gap-2"
                        style={{
                            background: "var(--bg-elev-2)",
                            borderColor: "var(--border-strong)",
                            color: "var(--text)",
                        }}
                        data-testid="withdraw-btn"
                    >
                        {pending === "WITHDRAW" ? (
                            <Loader2 size={16} className="animate-spin" />
                        ) : (
                            <ArrowUpRight size={16} style={{ color: "var(--warn)" }} />
                        )}
                        Withdraw
                    </Button>
                </div>

                {!wallet && (
                    <p
                        className="text-xs font-mono"
                        style={{ color: "var(--text-muted)" }}
                    >
                        Load a wallet first to enable operations.
                    </p>
                )}
            </div>
        </section>
    );
}

function ActivityFeed({ log }) {
    if (!log.length) {
        return (
            <section
                className="relative z-10 rounded-2xl p-6 md:p-8 animate-fade-up"
                style={{
                    background: "var(--bg-elev)",
                    border: "1px solid var(--border-soft)",
                }}
                data-testid="activity-feed-empty"
            >
                <h2 className="font-serif text-2xl mb-2">Activity</h2>
                <p
                    className="text-sm"
                    style={{ color: "var(--text-muted)" }}
                >
                    Your operations will appear here — this session only.
                </p>
            </section>
        );
    }

    return (
        <section
            className="relative z-10 rounded-2xl p-6 md:p-8 animate-fade-up"
            style={{
                background: "var(--bg-elev)",
                border: "1px solid var(--border-soft)",
            }}
            data-testid="activity-feed"
        >
            <h2 className="font-serif text-2xl mb-5">Activity</h2>
            <ul className="space-y-3">
                {log.map((entry) => {
                    const isOk = entry.status === "OK";
                    const isDeposit = entry.operationType === "DEPOSIT";
                    return (
                        <li
                            key={entry.id}
                            className="flex items-center gap-4 py-3 border-b last:border-0"
                            style={{ borderColor: "var(--border-soft)" }}
                            data-testid="activity-entry"
                        >
                            <div
                                className="h-9 w-9 rounded-full flex items-center justify-center flex-shrink-0"
                                style={{
                                    background: isOk
                                        ? "rgba(182,255,92,0.08)"
                                        : "rgba(255,107,107,0.08)",
                                    color: isOk ? "var(--accent)" : "var(--danger)",
                                }}
                            >
                                {isDeposit ? (
                                    <ArrowDownLeft size={16} />
                                ) : (
                                    <ArrowUpRight size={16} />
                                )}
                            </div>
                            <div className="flex-1 min-w-0">
                                <div className="flex items-baseline gap-2">
                                    <span className="font-medium">
                                        {entry.operationType}
                                    </span>
                                    <span
                                        className="font-mono text-xs"
                                        style={{
                                            color: isOk
                                                ? "var(--text-muted)"
                                                : "var(--danger)",
                                        }}
                                    >
                                        {isOk ? "success" : "failed"}
                                    </span>
                                </div>
                                {!isOk && (
                                    <div
                                        className="text-xs mt-0.5 truncate"
                                        style={{ color: "var(--danger)" }}
                                    >
                                        {entry.error}
                                    </div>
                                )}
                                <div
                                    className="text-xs font-mono mt-0.5"
                                    style={{ color: "var(--text-muted)" }}
                                >
                                    {new Date(entry.at).toLocaleTimeString()}
                                </div>
                            </div>
                            <div className="text-right">
                                <div
                                    className={`font-mono text-base ${
                                        isOk ? "" : "line-through"
                                    }`}
                                    style={{
                                        color: isOk
                                            ? isDeposit
                                                ? "var(--accent)"
                                                : "var(--warn)"
                                            : "var(--text-muted)",
                                    }}
                                >
                                    {isDeposit ? "+" : "−"}
                                    {formatAmount(entry.amount)}
                                </div>
                                {isOk && (
                                    <div
                                        className="text-xs font-mono"
                                        style={{ color: "var(--text-muted)" }}
                                    >
                                        bal {formatAmount(entry.balance)}
                                    </div>
                                )}
                            </div>
                        </li>
                    );
                })}
            </ul>
        </section>
    );
}

function Footer() {
    return (
        <footer
            className="relative z-10 mt-16 px-6 md:px-10 py-6 border-t flex flex-wrap items-center gap-2 text-xs font-mono"
            style={{
                borderColor: "var(--border-soft)",
                color: "var(--text-muted)",
            }}
            data-testid="app-footer"
        >
            <span>Vallet</span>
            <span className="divider-dot" />
            <span>Spring Boot 3</span>
            <span className="divider-dot" />
            <span>PostgreSQL</span>
            <span className="divider-dot" />
            <span>Liquibase</span>
            <span className="divider-dot" />
            <span>Optimistic locking</span>
        </footer>
    );
}

function App() {
    const [wallet, setWallet] = useState(null);
    const [log, setLog] = useState([]);
    const [refreshing, setRefreshing] = useState(false);

    const pushLog = (entry) =>
        setLog((prev) => [entry, ...prev].slice(0, 50));

    const handleRefresh = async () => {
        if (!wallet) return;
        setRefreshing(true);
        try {
            const data = await walletApi.getBalance(wallet.walletId);
            setWallet(data);
        } catch (err) {
            toast.error(extractError(err));
        } finally {
            setRefreshing(false);
        }
    };

    return (
        <div className="min-h-screen relative grain glow">
            <Toaster
                position="top-right"
                theme="dark"
                toastOptions={{
                    style: {
                        background: "var(--bg-elev-2)",
                        border: "1px solid var(--border-soft)",
                        color: "var(--text)",
                        fontFamily: "JetBrains Mono, ui-monospace, monospace",
                        fontSize: "13px",
                    },
                }}
            />

            <TopBar />

            <main className="relative z-10 max-w-6xl mx-auto px-6 md:px-10 pt-10 md:pt-16 pb-12">
                <div className="mb-10 md:mb-14 max-w-2xl">
                    <h1 className="font-serif text-5xl md:text-6xl leading-[1.05]">
                        A wallet that <em>never</em> drops a request.
                    </h1>
                    <p
                        className="mt-4 text-base md:text-lg leading-relaxed"
                        style={{ color: "var(--text-muted)" }}
                    >
                        Deposit, withdraw, and read balances against the
                        Spring Boot service. Optimistic locking plus retries
                        keep the ledger consistent at 1000 RPS per wallet.
                    </p>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-5 gap-6">
                    <div className="lg:col-span-3 space-y-6">
                        <WalletHeroCard
                            wallet={wallet}
                            loading={refreshing}
                            onRefresh={handleRefresh}
                        />
                        <OperationPanel
                            wallet={wallet}
                            onUpdated={(data) => setWallet(data)}
                            onLog={pushLog}
                        />
                    </div>
                    <div className="lg:col-span-2 space-y-6">
                        <WalletPicker
                            onPick={(data) => setWallet(data)}
                            onCreated={(data) => setWallet(data)}
                        />
                        <ActivityFeed log={log} />
                    </div>
                </div>
            </main>

            <Footer />
        </div>
    );
}

export default App;
