export enum ConformanceStatus {
    CONFORMANT = "CONFORMANT",
    NON_CONFORMANT = "NON_CONFORMANT",
    PARTIALLY_CONFORMANT = "PARTIALLY_CONFORMANT",
    NO_TRAFFIC = "NO_TRAFFIC",
}

export function asConformanceStatus(stringConformanceStatus: string): ConformanceStatus {
    return ConformanceStatus[stringConformanceStatus as keyof typeof ConformanceStatus];
}
