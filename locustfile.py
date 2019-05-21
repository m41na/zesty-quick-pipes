from locust import HttpLocust, TaskSet

"""
locust -f locustfile.py --host=http://localhost:1337

def crawl(loc):
    loc.client.get("/crawl?depth=1&target=https://www.msn.com/en-us")

class CrawlBehavior(TaskSet):
    tasks = {crawl: 1}

    def on_start(self):
        crawl(self)

class WebsiteUser(HttpLocust):
    task_set = CrawlBehavior
    min_wait = 5000
    max_wait = 9000
"""

def ping(loc):
	loc.client.get("/ping")
		
class PingBehavior(TaskSet):
	tasks = {ping: 1}
	
	def on_start(self):
		ping(self)

class WebsiteUser(HttpLocust):
    task_set = PingBehavior
    min_wait = 5000
    max_wait = 9000
        