# Frontend container interface

No frontend source or Dockerfile is present. When supplied, the frontend source should own its Dockerfile. Browser-originated API calls normally use `http://localhost:8080`; a container-to-container proxy uses `http://backend:8080`.
