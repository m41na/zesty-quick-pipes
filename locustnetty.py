from locust import HttpLocust, TaskSet

"""
locust -f locustnetty.py --master --host=http://localhost:8080
locust -f locustnetty.py --slave --host=http://localhost:8080
"""

def ping(loc):
	loc.client.get("/")
		
class PingBehavior(TaskSet):
	tasks = {ping: 1}
	
	def on_start(self):
		ping(self)

class WebsiteUser(HttpLocust):
    task_set = PingBehavior
    min_wait = 5000
    max_wait = 9000
        