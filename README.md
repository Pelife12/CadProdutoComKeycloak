# Equipe: André Ramos, Felipe Bogo, Jeison Diniz e Thierry Busnardo

Projeto desenvolvido para cadastro de um produto que será utilizado no PAC.

Instruções para configurar o Keycloak:

Abra o docker(caso seja o desktop) e rode o comando no terminal:
- docker run -p 127.0.0.1:8080:8080 -e KC_BOOTSTRAP_ADMIN_USERNAME=admin -e KC_BOOTSTRAP_ADMIN_PASSWORD=admin quay.io/keycloak/keycloak:26.3.0 start-dev

Após isso acesse o link https://www.keycloak.org/getting-started/getting-started-docker e na seção "Log in to the Admin Console" entre no admin console.

![image](https://github.com/user-attachments/assets/1f3f013c-43b9-442e-afea-34d07e440eee)

Ao acessar o admin console exporte o arquivo json realm-export presente no projeto.

![image](https://github.com/user-attachments/assets/2c41569f-7aed-4865-a4f5-3491e7c2f755)

![image](https://github.com/user-attachments/assets/0bc3b34e-0027-4e52-9941-433816172aad)


*Usuário criado para teste:*

Usuário: FelipeTeste
Senha: teste
