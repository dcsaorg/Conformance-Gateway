export interface Standard {
    name: string,
    versions: StandardVersion[],
}

export interface StandardVersion {
    number: string,
    suites: string[],
    roles: string[],
}
