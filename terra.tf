provider "aws" {
region = "us-east-1"
#access_key = "$(var.AWS_ACCESS_KEY_ID)"
#secret_key = "$(var.AWS_SECRET_ACCESS_KEY)"
}

resource "aws_instance" "linuxnix" {
count = 1
ami = "ami-0565af6e282977273"
instance_type = "t2.micro"
key_name = "AWS2"
tags {
Name = "Demo301"
}
}
