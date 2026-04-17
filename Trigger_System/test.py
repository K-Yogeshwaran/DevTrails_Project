
from redis_store import register_worker

register_worker(
    worker_id="test123",
    name="Test",
    persona="agent",
    zone_id="zone_1",
    lat=10.0,
    lon=20.0
)

print("Completed")