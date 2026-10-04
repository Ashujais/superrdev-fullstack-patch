const API_BASE = '/api';

export async function fetchTasks({ query = '', status = '', page = 1, pageSize = 10, signal }) {
  const params = new URLSearchParams();
  if (query) params.set('q', query);
  if (status) params.set('status', status);
  params.set('page', String(page));
  params.set('pageSize', String(pageSize));

  const url = `${API_BASE}/tasks?${params.toString()}`;
  console.log('[api] fetching:', url);

  const response = await fetch(url, { signal });

  if (!response.ok) {
    let errorMessage = `Request failed: ${response.status}`;
    try {
      const errorData = await response.json();
      if (errorData && errorData.error) {
        errorMessage = errorData.error;
      }
    } catch (_) {
      // Fall back to default error message if body is not JSON
    }
    throw new Error(errorMessage);
  }

  return response.json();
}
