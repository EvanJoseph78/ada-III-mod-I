param([int]$EmpregadosPort = 18081, [int]$VagasPort = 18082, [int]$CandidaturasPort = 18083)
$ErrorActionPreference = 'Stop'
$raiz = Split-Path $PSScriptRoot -Parent
$logs = Join-Path $raiz 'target/validacao-fluxo'
New-Item -ItemType Directory -Force -Path $logs | Out-Null
$iniciados = [Collections.Generic.List[Diagnostics.Process]]::new()

# O helper carrega explicitamente apenas credenciais locais necessárias; não as imprime.
$dotenv = Join-Path $raiz '.env'
if (Test-Path -LiteralPath $dotenv) {
  foreach ($linha in Get-Content -LiteralPath $dotenv) {
    if ($linha -match '^(EMPREGADOS_DB_PASSWORD|VAGAS_DB_PASSWORD|CANDIDATURAS_DB_PASSWORD|REDIS_PASSWORD)=(.*)$') {
      if (-not [Environment]::GetEnvironmentVariable($Matches[1])) {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2].Trim().Trim('"').Trim("'"), 'Process')
      }
    }
  }
}

function Api($metodo, $url, $corpo = $null, $esperado = 200) {
  $opcoes = @{ Method = $metodo; Uri = $url; UseBasicParsing = $true; TimeoutSec = 10 }
  if ($null -ne $corpo) {
    $opcoes.ContentType = 'application/json; charset=utf-8'
    $opcoes.Body = [Text.Encoding]::UTF8.GetBytes(($corpo | ConvertTo-Json -Depth 12))
  }
  try { $resposta = Invoke-WebRequest @opcoes }
  catch {
    if ($_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq $esperado) { return $null }
    throw
  }
  if ([int]$resposta.StatusCode -ne $esperado) { throw "HTTP inesperado: $($resposta.StatusCode), esperado $esperado em $url" }
  if ($resposta.Content) { return ($resposta.Content | ConvertFrom-Json) }
}

function Aguardar($descricao, [scriptblock]$condicao) {
  $limite = [DateTime]::UtcNow.AddSeconds(90)
  while ([DateTime]::UtcNow -lt $limite) {
    if (& $condicao) { return }
    Start-Sleep -Milliseconds 300
  }
  throw "Timeout: $descricao. Consultar $logs"
}

function Iniciar($servico, $porta, $argumentos, $sufixo) {
  $jar = Join-Path $raiz "$servico/target/$servico-0.1.0-SNAPSHOT.jar"
  if (-not (Test-Path -LiteralPath $jar)) { throw "Construir primeiro com mvn -PB clean verify: $jar" }
  $argsJava = @('-jar', ('"' + $jar + '"'), "--server.port=$porta") + $argumentos
  $processo = Start-Process -FilePath (Get-Command java).Source -ArgumentList $argsJava -WorkingDirectory $raiz `
      -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $logs "$servico-$sufixo.log") `
      -RedirectStandardError (Join-Path $logs "$servico-$sufixo.err.log")
  $iniciados.Add($processo)
  return $processo
}

function Parar([Diagnostics.Process]$processo) {
  if (-not $processo.HasExited) { $processo.Kill(); $processo.WaitForExit(10000) | Out-Null }
}

$emp = "http://localhost:$EmpregadosPort"
$vag = "http://localhost:$VagasPort"
$can = "http://localhost:$CandidaturasPort"
$argsVagas = @("--integracoes.candidaturas-url=$can")
$argsCand = @("--integracoes.empregados-url=$emp", "--integracoes.vagas-url=$vag")
try {
  if (@(@($EmpregadosPort, $VagasPort, $CandidaturasPort) | Select-Object -Unique).Count -ne 3) {
    throw 'Usar tres portas HTTP distintas.'
  }
  foreach ($porta in @($EmpregadosPort, $VagasPort, $CandidaturasPort)) {
    $probe = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, $porta)
    try { $probe.Start() } finally { $probe.Stop() }
  }
  Write-Output 'Iniciando processos exclusivos da verificacao; PostgreSQL/Kafka/Redis devem estar disponiveis.'
  $pEmp = Iniciar 'empregados-service' $EmpregadosPort @() 'inicial'
  $pVag = Iniciar 'vagas-service' $VagasPort ($argsVagas + '--mensageria.enabled=false') 'pausado'
  $pCan = Iniciar 'candidaturas-service' $CandidaturasPort ($argsCand + '--mensageria.enabled=false') 'pausado'
  Aguardar 'Empregados' { try { Api Get "$emp/api/v1/empregados/0" $null 404 | Out-Null; $true } catch { $false } }
  Aguardar 'Vagas' { try { Api Get "$vag/api/v1/vagas/0" $null 404 | Out-Null; $true } catch { $false } }
  Aguardar 'Candidaturas' { try { Api Get "$can/api/v1/candidaturas/00000000-0000-0000-0000-000000000000" $null 404 | Out-Null; $true } catch { $false } }
  $matricula = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
  $java = @{ tipo = 'CURSO'; descricao = 'Java'; horas = 40 }
  $spring = @{ tipo = 'CURSO'; descricao = 'Spring'; horas = 40 }
  Api Post "$emp/api/v1/empregados" @{matriculaEmpregado=$matricula; nomeEmpregado='Ana Demo'; itens=@($java)} 201 | Out-Null
  Api Post "$emp/api/v1/empregados" @{matriculaEmpregado=($matricula+1); nomeEmpregado='Bia Demo'; itens=@($java,$spring)} 201 | Out-Null
  Api Post "$emp/api/v1/empregados" @{matriculaEmpregado=($matricula+2); nomeEmpregado='Carlos Demo'; itens=@($java)} 201 | Out-Null
  $vaga = Api Post "$vag/api/v1/vagas" @{nomeVaga='Demo publicacao'; requisitos=@(
      @{tipo='CURSO'; palavraChave='Java'; obrigatorio=$true; pontos=10},
      @{tipo='CURSO'; palavraChave='Spring'; obrigatorio=$false; pontos=20})} 201
  $a = Api Post "$can/api/v1/candidaturas" @{matriculaEmpregado=$matricula; idVaga=$vaga.idVaga} 201
  $b = Api Post "$can/api/v1/candidaturas" @{matriculaEmpregado=($matricula+1); idVaga=$vaga.idVaga} 201
  Api Post "$vag/api/v1/vagas/$($vaga.vagaId)/resultado" $null 409 | Out-Null
  Api Post "$vag/api/v1/vagas/$($vaga.vagaId)/encerramento" | Out-Null
  Api Post "$can/api/v1/candidaturas" @{matriculaEmpregado=($matricula+2); idVaga=$vaga.idVaga} 409 | Out-Null
  Api Post "$vag/api/v1/vagas/$($vaga.vagaId)/resultado" $null 409 | Out-Null
  Write-Output 'APROVADO: fechamento bloqueia inscricao tardia e publicacao com avaliacao pendente.'
  Parar $pCan
  $pCan = Iniciar 'candidaturas-service' $CandidaturasPort $argsCand 'ativo'
  Aguardar 'avaliacoes apos reinicio' { try {
    (Api Get "$can/api/v1/candidaturas/$($a.candidaturaId)").status -eq 'AVALIADA' -and
    (Api Get "$can/api/v1/candidaturas/$($b.candidaturaId)").status -eq 'AVALIADA'
  } catch { $false } }
  $ranking = Api Get "$can/api/v1/candidaturas/ranking/$($vaga.vagaId)"
  if (($ranking.classificados.pontuacao -join ',') -ne '30,10') { throw 'Ranking inesperado' }
  Api Get "$can/api/v1/candidaturas/ranking/$($vaga.vagaId)" | Out-Null
  Api Post "$vag/api/v1/vagas/$($vaga.vagaId)/resultado" $null 202 | Out-Null
  Api Get "$can/api/v1/candidaturas/resultados/$($vaga.vagaId)" $null 404 | Out-Null
  Write-Output 'APROVADO: ranking 30/10; publicacao aceita e pendente com dispatcher de Vagas desativado.'
  Parar $pVag
  $pVag = Iniciar 'vagas-service' $VagasPort $argsVagas 'ativo'
  Aguardar 'resultado definitivo apos reiniciar Vagas' { try {
    (Api Get "$can/api/v1/candidaturas/resultados/$($vaga.vagaId)").publicacao.vagaId -eq $vaga.vagaId
  } catch { $false } }
  $resultado = Api Get "$can/api/v1/candidaturas/resultados/$($vaga.vagaId)"
  if (($resultado.ranking.classificados.pontuacao -join ',') -ne '30,10') { throw 'Resultado definitivo inesperado' }
  Parar $pCan
  $pCan = Iniciar 'candidaturas-service' $CandidaturasPort $argsCand 'reiniciado'
  Aguardar 'resultado persistido apos reiniciar Candidaturas' { try {
    (Api Get "$can/api/v1/candidaturas/resultados/$($vaga.vagaId)").publicacao.eventId -eq $resultado.publicacao.eventId
  } catch { $false } }
  $persistido = Api Get "$can/api/v1/candidaturas/resultados/$($vaga.vagaId)"
  if (($resultado | ConvertTo-Json -Depth 12 -Compress) -ne ($persistido | ConvertTo-Json -Depth 12 -Compress)) { throw 'Resultado mudou apos reinicio' }
  Api Post "$vag/api/v1/vagas/$($vaga.vagaId)/resultado" $null 409 | Out-Null
  $concluida = Api Post "$vag/api/v1/vagas/$($vaga.vagaId)/conclusao"
  if ($concluida.status -ne 'CONCLUIDA') { throw 'Status final inesperado' }
  $evidencia = @{vagaId=$vaga.vagaId; idVaga=$vaga.idVaga; matriculaAna=$matricula; matriculaBia=($matricula+1);
    candidaturaAna=$a.candidaturaId; candidaturaBia=$b.candidaturaId; resultado=$resultado; status=$concluida.status}
  $evidencia | ConvertTo-Json -Depth 12 | Set-Content -Encoding UTF8 (Join-Path $logs 'evidencia.json')
  Write-Output "APROVADO: publicacao retomada, resultado persistido apos reinicio e processo CONCLUIDA. Evidencia: $logs/evidencia.json"
} finally {
  foreach ($processo in $iniciados) { Parar $processo }
  Write-Output 'Processos criados pelo helper encerrados; infraestrutura e dados preservados.'
}
