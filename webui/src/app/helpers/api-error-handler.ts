export function handleApiCall(
    response: any,
): any {
    if (response?.error) {
        return {"error": response.message ? response.message : response.error};
    }
    return response;
}

