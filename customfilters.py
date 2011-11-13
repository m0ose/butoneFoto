from google.appengine.ext import webapp
 
register = webapp.template.create_template_register()
 
def slicestring(value, slice):
	splitslice = slice.split(",")
	return value[int(splitslice[0]):int(splitslice[1])]
	
def replacestring(value, replace):
	splitreplace = replace.split(",")
	return value.replace(splitreplace[0],splitreplace[1])
 
register.filter(slicestring)
register.filter(replacestring)