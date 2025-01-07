# Relatório do Projeto: Aplicação de Chat

## Estrutura do Projeto

De forma a preencher os requisitos do trabalho final da disciplina de **Desenvolvimento de Jogos para Plataformas Móveis**, foi decidido criar uma aplicação de chat utilizando a Firebase. A estrutura do projeto está organizada em três principais pastas (sendo o `Main` encontrado fora de qualquer pasta):

- **data**
- **ui**
- **util**

### Estrutura da pasta `data`
A pasta `data` contém:
- **local**: Contém funções relacionadas às preferências do utilizador, como guardar e apagar credenciais.
- **model**: Define os modelos de cada objeto utilizado na Firebase, como `User`, `Message` e `Group`. Apesar do modelo de grupos não ter sido concretizado, a estrutura foi planeada.
- **repository**: Contém as funções para interação com a Firebase, organizadas por responsabilidade:
  - **AuthRepository**: Funções relativas ao login e autenticação.
  - **MessageRepository**: Funções relativas ao envio, edição e gestão de mensagens.
  - **UserRepository**: Funções relativas à criação, busca, atualização e outras ações relacionadas a utilizadores.
- **Result.kt**: Define os possíveis estados de retorno das operações realizadas no projeto.

### Estrutura da pasta `ui`
A pasta `ui` está subdividida em cinco pastas principais:
- **auth**: Contém o código para login e registo. Implementado no modelo MVVM, cada funcionalidade possui um `.kt` e o respetivo `ViewModel`. Também inclui o `FirebaseMessagingService` para notificações (agora finalizado).
- **chat**: Código relativo à tela de chat.
- **profile**: Código relativo ao perfil do utilizador, permitindo atualização de dados.
- **theme**: Define as cores, fontes e estilos principais da aplicação.
- **users**: Código relativo ao ecrã principal, onde são exibidos todos os utilizadores disponíveis para iniciar conversas.

### Estrutura da pasta `util`
Esta pasta é utilizada para armazenar:
- Formatações de data.
- Funções auxiliares relacionadas com imagens.

---

## Lista de Funcionalidades

- **Login**:  
  - Verifica o formato do email e comprimento da palavra-passe.
  - Permite alternar entre exibir e ocultar a palavra-passe.
  - Apresenta avisos em caso de autenticação incorreta.

- **Registo**:  
  - Realiza as mesmas verificações que o login.
  - Adiciona um campo para confirmar a palavra-passe.

_Tanto o login como o registo utilizam a Firebase Email/Password Authentication._

- **Ecrã Principal**:  
  - Exibe todos os utilizadores na base de dados.
  - Permite procurar utilizadores específicos por email ou nome.
  - Exibe a última mensagem trocada e a foto do utilizador.

- **Perfil**:  
  - Permite a atualização do nome e da foto de perfil.
  - Possui funcionalidade de logout.

- **Chat Screen**:  
  - Comunicação em tempo real, incluindo mensagens e imagens.
  - Permite envio de várias imagens por mensagem.
  - Permite editar, copiar e eliminar mensagens.
  - Possui funcionalidade de busca em mensagens específicas no chat.
  - Notificações.

---

## Modelo de Dados

O modelo de dados utilizado está estruturado em três principais objetos:
1. **User**: Representa os dados do utilizador, incluindo email, nome, token FCM, URL da foto de perfil e data de criação.
2. **Message**: Representa as mensagens trocadas, incluindo remetente, destinatário, texto, timestamp, URLs de imagens e estado de leitura.
3. **Group**: Representa os grupos (planeado, mas não implementado completamente), com dados como ID, nome, membros e foto do grupo.

---

## Implementação do Projeto

A aplicação foi implementada utilizando o modelo MVVM (Model-View-ViewModel), com divisão clara entre lógica de negócio, modelos de dados e interface do utilizador. A Firebase foi utilizada como backend para autenticação, armazenamento de dados e mensagens em tempo real.

---

## Tecnologias Usadas

- **Linguagem:** Kotlin
- **Framework de UI:** Jetpack Compose
- **Backend:** Firebase (Firestore, Authentication, Firebase Cloud Messaging)
- **Gestão de Estado:** StateFlow (Kotlin Coroutines)
- **Armazenamento Local:** DataStore
- **Manipulação de Imagens:** Coil

---

## Dificuldades

- **Gestão de Mensagens:** Implementar a funcionalidade de busca, edição e eliminação de mensagens apresentou desafios, mas foi concluída com sucesso.
- **Gestão de Estados:** Garantir a consistência entre os estados da aplicação utilizando `Flow` e `StateFlow` foi inicialmente desafiador.

---

## Conclusões

O projeto foi uma experiência enriquecedora, permitindo explorar e consolidar conhecimentos no desenvolvimento de aplicações móveis utilizando Android Studio, Jetpack Compose e Firebase. A divisão clara de responsabilidades na estrutura do código tornou o projeto mais organizado e escalável. Apesar de algumas dificuldades, os objetivos principais foram alcançados com sucesso.
