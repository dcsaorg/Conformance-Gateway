/**
 * Helper function to handle API calls with automatic error handling.
 * The error dialog is shown by the API service, so this just catches the error
 * and optionally executes a success callback.
 *
 * @param apiCall - The async API call to execute
 * @param onSuccess - Optional callback to execute if the API call succeeds
 * @returns Promise<boolean> - Returns true if successful, false if error occurred
 */
export async function handleApiCall<T>(
  apiCall: () => Promise<T>,
  onSuccess?: (result: T) => void | Promise<void>
): Promise<boolean> {
  try {
    const result = await apiCall();
    if (onSuccess) {
      await onSuccess(result);
    }
    return true;
  } catch (error) {
    // Error dialog already shown by API service
    console.log('API call failed:', error);
    return false;
  }
}

