docker pull puckel/docker-airflow
mkdir -p dags
echo -e "AIRFLOW_UID=1000\nAIRFLOW_GID=1000" > .env
docker run -d -p 8080:8080 --label airflow=v1 -v $(pwd)/dags:/usr/local/airflow/dags puckel/docker-airflow webserver
docker ps -f "status=running" -f "label=airflow=v1" --format '{{.ID}}' | xargs docker logs -f
