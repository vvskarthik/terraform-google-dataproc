data "google_compute_zones" "available" {}

locals {
  cluster_name = "${format("%s-%s-%s", "dataproc", var.appname, var.region_name)}"
}

resource "random_id" "id" {
  byte_length = 4
}


resource "google_storage_bucket" "gcs_bucket" {
  project       = "${var.project_id}"
  name          = "dataproc-staging-bucket-${random_id.id.hex}"
  storage_class = "REGIONAL"
  location      = "${var.region_name}"
  force_destroy = "true"

}

resource "null_resource" "cluster" {
  # Changes to script will reprovision the cluster
  triggers = {
    scripts = "${sha1(file("${path.module}/config.sh"))}"
  }

  depends_on = ["google_storage_bucket.gcs_bucket"]

  provisioner "local-exec" {
    command = "gsutil cp ${path.module}/config.sh ${google_storage_bucket.gcs_bucket.url}/scripts/"

  }
}
resource "google_dataproc_cluster" "main" {
  depends_on = ["google_storage_bucket.gcs_bucket"]
  name       = "${local.cluster_name}"
  project    = "${var.project_id}"
  region     = "${var.region_name}"
  labels = {
    foo = "bar"
  }

  cluster_config {
    //delete_autogen_bucket = "${var.delete_autogen_bucket}"
    staging_bucket = "${google_storage_bucket.gcs_bucket.name}"



    master_config {
      num_instances = "${var.high_availability ? 3 : 1}"
      machine_type  = "${var.master_machine_type}"
      disk_config {
        boot_disk_size_gb = "${var.master_boot_disk_size_gb}"
      }
    }

    worker_config {
      num_instances = "${var.worker_num_instances}"
      machine_type  = "${var.worker_machine_type}"
      disk_config {
        boot_disk_size_gb = "${var.worker_boot_disk_size_gb}"
        num_local_ssds    = "${var.worker_num_local_ssds}"
      }
    }

    preemptible_worker_config {
      num_instances = "${var.preemptible_num_instances}"
    }

    # Override or set some custom properties
    software_config {
      image_version = "${var.image_version}"
      override_properties = {
        "dataproc:dataproc.allow.zero.workers" = "true"
        ## add dataproc properties 
      }
    }

    gce_cluster_config {
      zone             = "${data.google_compute_zones.available.names[0]}"
      internal_ip_only = "false"
      #network = "${google_compute_network.dataproc_network.name}"

      #TODO
      #tags    = ["foo", "bar"]
    }

    initialization_action {
      script      = "gs://${google_storage_bucket.gcs_bucket.name}/scripts/config.sh"
      timeout_sec = 500
    }

  }
}
