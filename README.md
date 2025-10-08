## Testing the code

### User Registration

curl --location 'http://localhost:8080/register' \
--header 'Content-Type: application/json' \
--data-raw '{
"firstName": "Riya",
"lastName": "vishnoi",
"email": "pragya42@gmail.com"
}'

### User Registration in BULK

curl --location 'http://localhost:8080/register/bulk' \
--header 'Content-Type: application/json' \
--data-raw '[{
"firstName": "Pragya",
"lastName": "vishnoi",
"email": "test7@gmail.com"
},
{
"firstName": "Riya",
"lastName": "vishnoi",
"email": "test8@gmail.com"  
}]'

### UserName Retrieval from Password

curl --location 'http://localhost:8080/users/retrieve' \
--header 'Content-Type: application/json' \
--data '{

    "password": "xf3bKsn06hsSrFNmXjbi0w=="
}'

### Updation

curl --location --request PUT 'http://localhost:8080/update' \
--header 'Content-Type: application/json' \
--data-raw '{
"username": "riya",
"password": "xf3bKsn06hsSrFNmXjbi0w==",
"firstName": "Pragya",
"lastName": "",
"email": "pragyavishnoi42@gmail.com"
}'


### Deletion

curl --location --request DELETE 'http://localhost:8080/delete' \
--header 'Content-Type: application/json' \
--data '{
"password": "0a20a4044574",
"username": "pragya1"
}'