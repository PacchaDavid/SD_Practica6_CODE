FROM python:3.11-slim
WORKDIR /app
COPY frontend/ ./
RUN pip install --no-cache-dir --upgrade pip
EXPOSE 3000
CMD ["python", "server.py"]
