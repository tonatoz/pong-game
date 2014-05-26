# Pong game

Курсовая (4-неделя) для курса по Clojure http://clojurecourse.by/

Демо - http://pong.tonatoz.com/

## Описалово

Многопользовательская онлайн игра в пинг-понг.
Сервер и клиент на Clojure.

## Как пользоваться то?

При входе надо указать имя (оно проверяеться игрой на уникальность).

На следующем экране будет виден список оппонентов. Любого из них можно вызвать на бой.
Игроки участвующие в партии удаляться из этого списка, по оконччанию игры попадают в него снова.

В игре вызвовший игрок управляет левой платвормой, вызываемый правой. управление происходит с помощью движения мыши.

## Как запустить в прод.

Отдеактировать параметры файлов `init.sh` и `run.sh`. Запустить `./init.sh start`.

Раздавать через Nginx.

```
upstream cl_pong {
  server 127.0.0.1:3000;  
  keepalive 32;
}

server {
  server_name pong.***.com;
  gzip on;  
  keepalive_timeout 256;

  location / {
    proxy_pass  http://cl_pong;
    proxy_http_version 1.1;
    proxy_set_header Connection "";
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    proxy_set_header Host $http_host;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    access_log  /var/log/nginx/cl_pong.access.log;
  }
}
```