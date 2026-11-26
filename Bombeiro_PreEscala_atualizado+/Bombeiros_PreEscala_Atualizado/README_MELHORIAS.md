# Pré-Escala — melhorias implementadas

Este pacote parte do projeto original e preserva os módulos existentes. As mudanças estão concentradas em validações de reserva, senha, calendário, manual, exportação e recuperação de senha.

## Regras implementadas
- Bloqueio de identificador de login composto apenas por números (o sistema usa e-mail como login).
- Senhas novas com mínimo de 8 caracteres, letra maiúscula, minúscula, número e caractere especial; bloqueia sequências/senhas óbvias comuns.
- Máximo de 3 reservas ativas por bombeiro na mesma data.
- Reserva bloqueada quando faltar menos de 24 horas para o início do plantão (turnos: 07h, 13h e 19h).
- Confirmação visual após cadastro administrativo e após reserva; mensagens temporárias.
- Seleção direta do mês/ano na tela de escalas.
- Manual acessível em `/manual`.
- Ícone para mostrar/ocultar senha no login, cadastro administrativo e redefinição.
- Aviso de antecedência mínima no formulário de reserva (verifique a tela antes de confirmar).
- Exportação Excel `.xlsx` por mês, disponível na área administrativa.
- Recuperação de senha por link enviado ao e-mail cadastrado, com token válido por 30 minutos.

## Configuração obrigatória para recuperação por e-mail
Configure as variáveis de ambiente SMTP antes de usar a recuperação:

- `SMTP_HOST`
- `SMTP_PORT`
- `SMTP_USERNAME`
- `SMTP_PASSWORD`
- `SMTP_AUTH` (padrão `true`)
- `SMTP_STARTTLS` (padrão `true`)
- `APP_URL` (endereço público do sistema; ex.: `https://seu-dominio`)

Sem SMTP válido, a solicitação não enviará e-mail. Não coloque credenciais SMTP diretamente no código ou no Git.

## Banco de dados
A entidade `PasswordResetToken` adiciona a tabela `password_reset_token`. Com `spring.jpa.hibernate.ddl-auto=update`, o Hibernate deve criá-la. Também foi incluído `database/atualizacao_recuperacao_senha.sql` para implantação manual. Faça backup do banco antes de atualizar.

## Executar
Na pasta que contém `pom.xml`, execute no Windows PowerShell:

```powershell
.\mvnw.cmd spring-boot:run
```

Ou compile com:

```powershell
.\mvnw.cmd clean package
```

É necessário ter Java compatível com a versão configurada no `pom.xml` (Java 25), acesso ao MySQL e dependências Maven disponíveis.

## Observação de validação
O ambiente de edição não tinha Maven instalado e o Maven Wrapper não conseguiu baixar o Maven por indisponibilidade de rede. Portanto, a compilação final precisa ser executada no computador de desenvolvimento, com acesso ao Maven Central. O ZIP original foi mantido intacto; este é um novo pacote.
