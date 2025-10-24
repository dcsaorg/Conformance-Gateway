export enum ConformanceStatus {
    CONFORMANT = "CONFORMANT",
    NON_CONFORMANT = "NON_CONFORMANT",
    PARTIALLY_CONFORMANT = "PARTIALLY_CONFORMANT",
    NO_TRAFFIC = "NO_TRAFFIC",
    IRRELEVANT= "IRRELEVANT",
}

export function asConformanceStatus(stringConformanceStatus: string): ConformanceStatus {
    return ConformanceStatus[stringConformanceStatus as keyof typeof ConformanceStatus];
}

export function getConformanceStatusEmoji(conformanceStatus: ConformanceStatus): string {
    switch (conformanceStatus) {
        case ConformanceStatus.CONFORMANT:
            return "✅";
        case ConformanceStatus.NON_CONFORMANT:
            return "🚫";
        case ConformanceStatus.PARTIALLY_CONFORMANT:
            return "✔️";
        case ConformanceStatus.NO_TRAFFIC:
            return "❔";
        case ConformanceStatus.IRRELEVANT:
            return "➖";
    }
}

export function getConformanceStatusTitle(conformanceStatus: ConformanceStatus): string {
    switch (conformanceStatus) {
        case ConformanceStatus.CONFORMANT:
            return "Conformant";
        case ConformanceStatus.NON_CONFORMANT:
            return "Non-conformant";
        case ConformanceStatus.PARTIALLY_CONFORMANT:
            return "Partially conformant";
        case ConformanceStatus.NO_TRAFFIC:
            return "No traffic";
        case ConformanceStatus.IRRELEVANT:
            return "Irrelevant";
    }
}
