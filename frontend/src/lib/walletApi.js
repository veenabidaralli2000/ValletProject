import axios from "axios";

const BASE_URL = process.env.REACT_APP_BACKEND_URL || "http://localhost:8080";

const client = axios.create({
    baseURL: BASE_URL,
    headers: { "Content-Type": "application/json" },
    timeout: 15000,
});

export const walletApi = {
    baseUrl: BASE_URL,

    async createWallet({ walletId, initialBalance } = {}) {
        const payload = {};
        if (walletId) payload.walletId = walletId;
        if (initialBalance !== undefined && initialBalance !== null && initialBalance !== "")
            payload.initialBalance = Number(initialBalance);
        const { data } = await client.post("/api/v1/wallets", payload);
        return data;
    },

    async getBalance(walletId) {
        const { data } = await client.get(`/api/v1/wallets/${walletId}`);
        return data;
    },

    async operate({ walletId, operationType, amount }) {
        const { data } = await client.post("/api/v1/wallet", {
            walletId,
            operationType,
            amount: Number(amount),
        });
        return data;
    },
};

export function extractError(err) {
    if (err?.response?.data?.message) {
        const fe = err.response.data.fieldErrors;
        if (Array.isArray(fe) && fe.length) {
            return `${err.response.data.message}: ${fe
                .map((f) => `${f.field} — ${f.message}`)
                .join("; ")}`;
        }
        return err.response.data.message;
    }
    if (err?.message) return err.message;
    return "Unexpected error";
}
