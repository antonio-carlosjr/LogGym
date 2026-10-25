# S5 — backup lógico e recuperação de restauração

Decisão aprovada pelo responsável; implementação iniciada em 25/10/2026. Escopo: 34 pontos e 5 de reserva. Sem criptografia/senha, sincronização ou transferência automática. Esta decisão complementa S1-04 e preserva suas regras de domínio.

## Formato e privacidade

Extensão `.loggym`, UTF-8, JSON por linha, versão 1. Cabeçalho obrigatório: identificação, versão, modalidade, data em milissegundos, versão do app, fingerprint do catálogo, pisos dos alocadores e preferências gerenciadas. Cada registro identifica uma das sete tabelas de negócio por whitelist local; todos os campos são explícitos, inclusive nulos. A tabela de journal nunca é exportada. Rodapé obrigatório contém contagens por tabela e SHA-256 dos bytes anteriores, incluindo LF. SHA detecta corrupção acidental, sem autenticar o produtor.

Limites de leitura e escrita: 64 MiB por arquivo, 1 MiB por linha e 500 mil registros de negócio. Não truncar. Versão/catalogo desconhecidos, JSON com propriedades repetidas, tipos/campos/enums inválidos, checksum incorreto e relacionamentos inconsistentes são recusados. Validação ocorre em banco privado temporário com o esquema atual, antes de qualquer escrita nos dados do usuário.

| Modalidade | Conteúdo |
| --- | --- |
| Completo | Catálogo nativo com chaves estáveis/observações, personalizados, planos, divisões, prescrições, preferências, histórico e rascunho com valores/checks/timestamps |
| Sem histórico | Mesmo conjunto de configuração, sem qualquer sessão ou série executada |

IDs, referências históricas fracas, snapshots, posições com lacunas, extras e nulos/zeros são preservados. Os pisos de `sqlite_sequence` não recuam abaixo do estado importado nem do destino. A compatibilidade é da versão de formato/catálogo reconhecida; futuras versões exigem adaptação explícita. Não existe promessa de importação em versões antigas do aplicativo.

O aviso é: “Este arquivo não está criptografado. Quem tiver acesso a ele poderá ler seus dados.” Após refinamento solicitado em 25/10/2026, aparece no diálogo de informação junto ao título e no modal de exportação, junto das modalidades separadas por linha em branco; não há bloco explicativo fixo na seção. Exportação lógica não copia o banco aberto. SQL e caminhos nunca são obtidos do arquivo.

## Salvamento e captura

`PersistenceCoordinator` coordena todas as mutações dos repositórios e preferências. Cada ViewModel captura uma geração; comandos aceitos são registrados antes de iniciar. O editor da sessão registra um participante com preparação suspensa/lease liberável e falhas tipadas de entrada, armazenamento ou mudança de proprietário; cancelamento usa o contrato de corrotinas.

Ordem: bloquear admissão de edições/comandos → aguardar comandos aceitos → cancelar agendamentos de debounce, aguardar mutex local, validar/salvar revisões atuais → manter leases dos editores → adquirir exclusão global → capturar Room e preferências em arquivo privado completo → liberar tudo em `finally`. Nunca solicitar flush sob o mutex global. Uma transação antiga não reconhece uma revisão posterior como salva. Cancelamento na troca de dispatcher não pode perder uma lease adquirida.

Isso vale para ambas as modalidades. Sem histórico também persiste a sessão atual, mas não a exporta. Formulários com Salvar explícito permanecem não confirmados. Entrada inválida ou falha impede abrir o destino. A preparação não navega, marca séries, reaplica pré-preenchimento nem altera descanso. Autossalvamentos concluídos não são desfeitos ao cancelar o seletor.

## Substituição e recuperação

Room continua na biblioteca atual. Esquema 3 acrescenta somente `restore_journal`, com migração 2→3 e cadeia 1→2→3. A transação Room não abrange DataStore.

1. Copiar arquivo selecionado para área privada, validar integralmente e apresentar resumo. Confirmação substitui tudo; sem histórico explicita remoção do histórico e rascunho atuais.
2. Preparar editores, drenar gravações, adquirir exclusão e invalidar a geração anterior. Bloquear telas normais durante substituição/recuperação.
3. Capturar undo lógico completo com preferências anteriores e onboarding, em `noBackupFilesDir`, finalizado com `AtomicFile` e sincronização do descritor. Seu SHA fica no journal; caminho deriva de UUID local.
4. Na mesma transação Room: substituir sete tabelas de negócio, preservar pisos dos alocadores e inserir `APPLYING`.
5. Substituir preferências em um único `edit` DataStore e confirmar leitura.
6. Marcar `COMMITTED` em transação Room: ponto durável de sucesso.
7. Descartar pilha/ViewModels antigos e abrir Home → Configurações com resultado. Limpar arquivos/journal apenas em estado final seguro.

| Estado persistido ao iniciar | Resultado |
| --- | --- |
| Sem journal, antes do commit Room | Dados anteriores permanecem; artefatos órfãos são descartados |
| APPLYING | Repor tabelas de negócio do undo e preferências anteriores; confirmar ambos antes de ROLLED_BACK |
| Reversão interrompida | Repetir idempotentemente; não remover journal/undo antes da conclusão |
| COMMITTED | Manter dados importados; concluir limpeza |
| ROLLED_BACK | Manter dados anteriores; concluir limpeza |
| Undo inválido/inacessível ou outro erro de recuperação | Preservar evidências, bloquear conteúdo normal, oferecer nova tentativa |

Uma falha tratada antes do ponto final tenta reverter sob `NonCancellable`. Falha na reversão bloqueia o aplicativo até recuperação. A inicialização recupera antes de disponibilizar rotas normais. Não há fallback destrutivo. Os alocadores podem permanecer mais altos após reversão: isso impede reutilização de identidades e não altera registros anteriores.

As garantias cobrem erros tratados, encerramento do processo e reinício sob contratos de persistência do Android. Não cobrem perda física de armazenamento nem exclusão dos dados do app. Undo também respeita os limites de arquivo; falta de espaço ou conjunto anterior excessivo impede iniciar a substituição.

## Arquivos, ciclo de vida e interface

Seção Backup e restauração após Aparência/Preferências em Configurações. SAF (`CreateDocument`/`OpenDocument`), sem permissões amplas. O seletor de destino só abre após arquivo privado íntegro; sucesso externo somente depois de escrever e fechar sem erro. Na falha, tentar remover documento parcial, sem garantia em todo provider; nunca excluir origem importada.

Jobs pertencem ao repositório da aplicação: sobrevivem à rotação, sem trabalho em segundo plano ou retomada de transferência após morte do processo. Callbacks conferem token/etapa para impedir repetição. Não manter mutex durante seletor/cópia externa. Arquivos privados órfãos são descartados ao inicializar quando não há recuperação pendente.

Backup/transferência automática do Android excluem banco, preferências e arquivos, com regras anteriores e posteriores à API 31; `allowBackup=false`. A área `noBackupFilesDir` também é excluída pelo Android. Referências: [SAF](https://developer.android.com/training/data-storage/shared/documents-files) e [Auto Backup](https://developer.android.com/identity/data/autobackup).

## Verificação

Codec/validação, coordenação/flush e regressões da sessão têm testes próprios. `BackupEngineTest` exercita Room/DataStore reais, modalidades, destinos limpos/ocupados, referências fracas, alocadores, erro e interrupção em fronteiras do protocolo e da reversão. `SessionMigrationTest` verifica 1→2→3 e 2→3. Os testes Compose cobrem contratos SAF, confirmação, bloqueio de ações e substituição com recriação da navegação. Evidência automatizada não substitui [homologação manual](../HOMOLOGACAO-S5.md).
