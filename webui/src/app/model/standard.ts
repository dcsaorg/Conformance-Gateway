export interface Standard {
    name: string,
    versions: StandardVersion[],
}

export interface StandardVersion {
    number: string,
    roles: string[],
}
