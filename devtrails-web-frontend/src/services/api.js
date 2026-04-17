import axios from "axios";

const API = axios.create({
    baseURL: import.meta.env.BACKEND_API_URL || "http://localhost:8080/api",
});

// Add token automatically
API.interceptors.request.use((req) => {
    const token = localStorage.getItem("token");
    if (token) {
        req.headers.Authorization = `Bearer ${token}`;
    }
    return req;
});

export default API;