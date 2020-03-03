provider "google" {
  credentials = "${file("key.json")}"
  version     = "~>2.5.0"
  project     = "projectname"
  region      = "us-east1"
}



provider "random" {
  version = "~> 2.1"
}
