const BASE_URL = '/file';

// Utility: HTML-escape untrusted strings before innerHTML interpolation
function escapeHtml(str) {
    const div = document.createElement('div');
    div.textContent = String(str);
    return div.innerHTML;
}

// DOM Elements
const apiKeyInput = document.getElementById('apiKey');
const uploadForm = document.getElementById('uploadForm');
const fileInput = document.getElementById('fileInput');
const fileDropArea = document.getElementById('fileDropArea');
const fileInfo = document.getElementById('fileInfo');
const uploadBtn = document.getElementById('uploadBtn');
const refreshListBtn = document.getElementById('refreshListBtn');
const fileList = document.getElementById('fileList');
const responseViewer = document.getElementById('responseViewer');

// Utility: Get Headers
function getHeaders() {
    return {
        'X-API-KEY': apiKeyInput.value.trim()
    };
}

// Utility: Update Response Viewer
function showResponse(method, endpoint, status, body, duration) {
    const statusClass = status >= 200 && status < 300 ? 'status-ok' : status >= 400 ? 'status-error' : 'status-warn';
    let bodyText = typeof body === 'object' ? JSON.stringify(body, null, 2) : body;
    
    // If it's a blob/buffer or too long
    if (body instanceof Blob) {
        bodyText = `[Blob data: ${body.size} bytes, type: ${body.type}]`;
    }

    responseViewer.classList.remove('empty');
    responseViewer.innerHTML = `
        <div class="res-header">
            <span class="res-method">${escapeHtml(method)}</span>
            <span>${escapeHtml(endpoint)}</span>
            <span class="res-status ${statusClass}">${escapeHtml(status)}</span>
            ${duration ? `<span>${escapeHtml(duration)}ms</span>` : ''}
        </div>
        <div class="res-body">${escapeHtml(bodyText)}</div>
    `;
}

// Utility: Show Loading
function setLoading(isLoading) {
    if (isLoading) {
        document.body.style.cursor = 'wait';
        uploadBtn.disabled = true;
    } else {
        document.body.style.cursor = 'default';
        uploadBtn.disabled = false;
    }
}

// Handle File Drop Area
fileInput.addEventListener('change', (e) => {
    const file = e.target.files[0];
    if (file) {
        const sizeKB = (file.size / 1024).toFixed(2);
        fileInfo.textContent = `Selected: ${file.name} (${sizeKB} KB, ${file.type || 'Unknown Type'})`;
        fileInfo.classList.remove('hidden');
    } else {
        fileInfo.classList.add('hidden');
    }
});

fileDropArea.addEventListener('dragover', (e) => {
    e.preventDefault();
    fileDropArea.classList.add('drag-over');
});

fileDropArea.addEventListener('dragleave', () => {
    fileDropArea.classList.remove('drag-over');
});

fileDropArea.addEventListener('drop', (e) => {
    e.preventDefault();
    fileDropArea.classList.remove('drag-over');
    if (e.dataTransfer.files.length > 0) {
        fileInput.files = e.dataTransfer.files;
        fileInput.dispatchEvent(new Event('change'));
    }
});

// Upload File
uploadForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    if (!fileInput.files[0]) return;

    const formData = new FormData();
    formData.append('file', fileInput.files[0]);

    setLoading(true);
    const start = performance.now();
    try {
        const res = await fetch(BASE_URL, {
            method: 'POST',
            headers: getHeaders(), // Don't set Content-Type for FormData
            body: formData
        });
        const duration = Math.round(performance.now() - start);
        const text = await res.text();
        
        showResponse('POST', BASE_URL, res.status, text, duration);
        
        if (res.ok) {
            uploadForm.reset();
            fileInfo.classList.add('hidden');
            fetchFiles(); // Refresh list
        }
    } catch (err) {
        showResponse('POST', BASE_URL, 'ERROR', err.message);
    } finally {
        setLoading(false);
    }
});

// List Files
async function fetchFiles() {
    setLoading(true);
    const start = performance.now();
    try {
        const res = await fetch(`${BASE_URL}/list`, {
            headers: getHeaders()
        });
        const duration = Math.round(performance.now() - start);
        
        if (!res.ok) {
            const text = await res.text();
            showResponse('GET', `${BASE_URL}/list`, res.status, text, duration);
            fileList.innerHTML = `<li class="empty-state">Failed to load files (Status: ${res.status})</li>`;
            return;
        }

        const files = await res.json();
        showResponse('GET', `${BASE_URL}/list`, res.status, files, duration);
        renderFileList(files);
    } catch (err) {
        showResponse('GET', `${BASE_URL}/list`, 'ERROR', err.message);
        fileList.innerHTML = `<li class="empty-state">Network Error</li>`;
    } finally {
        setLoading(false);
    }
}

function renderFileList(files) {
    if (!files || files.length === 0) {
        fileList.innerHTML = `<li class="empty-state">No files stored.</li>`;
        return;
    }

    fileList.innerHTML = files.map(file => {
        const safe = escapeHtml(file);
        return `
        <li>
            <div class="file-name">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M13 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z"></path>
                    <polyline points="13 2 13 9 20 9"></polyline>
                </svg>
                ${safe}
            </div>
            <div class="file-actions">
                <button class="btn action-btn" onclick="fetchMetadata('${safe}')">Info</button>
                <button class="btn action-btn" onclick="downloadFile('${safe}')">Download</button>
                <button class="btn action-btn delete" onclick="deleteFile('${safe}')">Delete</button>
            </div>
        </li>
    `;
    }).join('');
}

// Fetch Metadata
window.fetchMetadata = async (filename) => {
    const endpoint = `${BASE_URL}/info/${filename}`;
    const start = performance.now();
    try {
        const res = await fetch(endpoint, { headers: getHeaders() });
        const duration = Math.round(performance.now() - start);
        const text = await res.text();
        try {
            const json = JSON.parse(text);
            showResponse('GET', endpoint, res.status, json, duration);
        } catch {
            showResponse('GET', endpoint, res.status, text, duration);
        }
    } catch (err) {
        showResponse('GET', endpoint, 'ERROR', err.message);
    }
};

// Download File
window.downloadFile = async (filename) => {
    const endpoint = `${BASE_URL}/${filename}`;
    const start = performance.now();
    try {
        const res = await fetch(endpoint, { headers: getHeaders() });
        const duration = Math.round(performance.now() - start);
        
        if (!res.ok) {
            const text = await res.text();
            showResponse('GET', endpoint, res.status, text, duration);
            return;
        }

        const blob = await res.blob();
        showResponse('GET', endpoint, res.status, blob, duration);
        
        // Trigger download
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        a.remove();
    } catch (err) {
        showResponse('GET', endpoint, 'ERROR', err.message);
    }
};

// Delete File
window.deleteFile = async (filename) => {
    if (!confirm(`Are you sure you want to delete ${filename}?`)) return;
    
    const endpoint = `${BASE_URL}/${filename}`;
    const start = performance.now();
    try {
        const res = await fetch(endpoint, {
            method: 'DELETE',
            headers: getHeaders()
        });
        const duration = Math.round(performance.now() - start);
        const text = await res.text();
        
        showResponse('DELETE', endpoint, res.status, text, duration);
        
        if (res.ok) {
            fetchFiles(); // Refresh list after successful delete
        }
    } catch (err) {
        showResponse('DELETE', endpoint, 'ERROR', err.message);
    }
};

// Event Listeners
refreshListBtn.addEventListener('click', fetchFiles);

// Initial Load
document.addEventListener('DOMContentLoaded', fetchFiles);
