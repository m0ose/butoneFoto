import os
import logging
from google.appengine.ext.webapp import template

import cgi

from google.appengine.api import users
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.ext import db

"""Load custom Django template filters"""
webapp.template.register_template_library('customfilters')

class Geotag(db.Model):
	user = db.StringProperty()
	tag = db.StringProperty()
	lat = db.StringProperty()
	lon = db.StringProperty()
	date = db.DateTimeProperty(auto_now_add=True)
	imgUrl = db.StringProperty()

class MainPage(webapp.RequestHandler):
    def get(self):
        
        geotags_query = Geotag.all()
        geotags = []
        users = []
        tags = []
        imgUrls = []
        
        for geotag in geotags_query:
        	user = geotag.user
        	tag = geotag.tag
        	imgUrl = geotag.imgUrl
        	if (user not in users): users.append(user)
        	if (tag not in tags): tags.append(tag)
        	if (imgUrl not in imgUrls): imgUrls.append(imgUrl)
        
        users.sort()
        tags.sort()
        
        
        template_values = {
            'geotags': geotags,
            'users': users,
            'tags': tags,
            'imgUrls': imgUrls,
        	}

        path = os.path.join(os.path.dirname(__file__), 'index.html')
        self.response.out.write(template.render(path, template_values))
    
class Upload(webapp.RequestHandler):
	def post(self):
		lat = self.request.get("lat")
		lon = self.request.get("lon")
		tag = self.request.get("tag")
		user = self.request.get("username")
		imgUrl =  self.request.get("imgUrl")
		
		geotag = Geotag()
		geotag.lat = cgi.escape(lat);
		geotag.lon = cgi.escape(lon);
		geotag.tag = cgi.escape(tag);
		geotag.user = cgi.escape(user);
		geotag.imgUrl = imgUrl;
		geotag.put()
		
class BrowseHandler(webapp.RequestHandler):
	def get(self, filterName, filterValue):
	
		filterName = filterName.replace("%20"," ");
		filterValue = filterValue.replace("%20", " ");
		
		# Prepare the query based on the groups of the regex
		geotags_query = Geotag.all()
		geotags_query.filter(filterName + " =", filterValue)
		geotags_query.order("date")
		
		# Prepare the data to pass to the template
		#geotags = [geotag for geotag in geotags_query]
		geotags = []
		for geotag in geotags_query:
			if len(geotag.imgUrl) and " " in geotag.imgUrl:
				geotag.imgUrl = str()
			geotags.append(geotag);
		
		displayData = []
		dataType = ""
		mapsQuery = ""
		
		logging.debug("filterName: %s, filterValue: %s", filterName, filterValue)
		
		if (filterName == "user"):
			tags = list(set([geotag.tag for geotag in geotags]))
			dataType = "tag"
			displayData = [[geotag for geotag in geotags if geotag.tag == tag] for tag in tags]
			mapsQuery = ""
		elif (filterName == "tag"):
			users = list(set([geotag.user for geotag in geotags]))
			dataType = "user"
			displayData = [[geotag for geotag in geotags if geotag.user == user] for user in users]

		
		template_values = { 'geotags': geotags,
							'filterName': filterName,
							'filterValue': filterValue,
							'dataType': dataType,
							'displayData': displayData }
		
		path = os.path.join(os.path.dirname(__file__), 'browse.html')
		self.response.out.write(template.render(path, template_values))

class AboutPage(webapp.RequestHandler):
	def get(self):
	
		template_values = {
		}
	
		path = os.path.join(os.path.dirname(__file__), 'about.html')
		self.response.out.write(template.render(path, template_values))
		
class DownloadPage(webapp.RequestHandler):
	def get(self):
	
		template_values = {
		}
	
		path = os.path.join(os.path.dirname(__file__), 'download.html')
		self.response.out.write(template.render(path, template_values))
		
class CreateMap(webapp.RequestHandler):
	def get(self, user, tag):
		a = 1
		
class CreateKml(webapp.RequestHandler):
	def get(self, user, tag):		
		# Prepare the query based on the groups of the regex
		geotags_query = Geotag.all()
		if (user != "" and user != "null"):
			logging.debug('The user is %s',user)
			geotags_query.filter("user =", user.replace("_"," "))
		if (tag != ""):
			geotags_query.filter("tag =", tag.replace("_"," "))

		geotags = [geotag for geotag in geotags_query]
		
		template_values = {'geotags': geotags}
		
		self.response.headers['Content-Type'] = 'application/vnd.google-earth.kml+xml'
		
		path = os.path.join(os.path.dirname(__file__), 'tagmap.kml')
		self.response.out.write(template.render(path, template_values))	
		
class TaxiApp(webapp.RequestHandler):
	def get(self):
		template_values = {
		}
	
		path = os.path.join(os.path.dirname(__file__), 'taxiapp.html')
		self.response.out.write(template.render(path, template_values))
		
class  Generate(webapp.RequestHandler):
	def get(self):
		geotag = Geotag()
		geotag.lat = "5";
		geotag.lon = "5";
		geotag.tag = "Debug Tag";
		geotag.user = "Debug User";
		geotag.imgUrl = "Debug url";
		geotag.put()

application = webapp.WSGIApplication(
                                     [('/', MainPage),
                                      ('/upload', Upload),
                                      (r'/browse/(.*)/(.*)', BrowseHandler),
                                      ('/about', AboutPage),
                                      (r'/map/(.*)/(.*)', CreateMap),
                                      (r'/kml/(.*)/(.*)', CreateKml),
                                      ('/generate', Generate),
                                      ('/download', DownloadPage),
                                      ('/taxiapp', TaxiApp)
                                      ],
                                     debug=True)
def testUser():
	geotag = Geotag()
	geotag.lat = "5";
	geotag.lon = "5";
	geotag.tag = "taco";
	geotag.user = "de bug";
	geotag.imgUrl = "no image";
	geotag.put()
	
def main():
    run_wsgi_app(application)
    testUser()

if __name__ == "__main__":
    main()
