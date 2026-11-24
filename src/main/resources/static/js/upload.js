// Tela de envio: mostra o arquivo escolhido, destaca a área ao arrastar
// e trava o botão com "Processando…" enquanto o servidor trabalha (OCR + IA levam alguns segundos).
(function () {
    const LIMITE_BYTES = 10 * 1024 * 1024; // mesmo limite do servidor (10MB)
    const TEXTO_PADRAO = 'Arraste o arquivo aqui ou clique para escolher';

    const form = document.getElementById('form-envio');
    const input = document.getElementById('arquivo');
    const area = document.getElementById('area-upload');
    const titulo = document.getElementById('upload-titulo');
    const aviso = document.getElementById('aviso-arquivo');
    const botao = document.getElementById('botao-enviar');
    const botaoTexto = document.getElementById('botao-texto');
    if (!form || !input) return;

    function tamanhoLegivel(bytes) {
        if (bytes < 1024 * 1024) return Math.max(1, Math.round(bytes / 1024)) + ' KB';
        return (bytes / (1024 * 1024)).toFixed(1).replace('.', ',') + ' MB';
    }

    function mostrarAviso(mensagem) {
        aviso.textContent = mensagem;
        aviso.hidden = !mensagem;
    }

    function atualizarArquivo() {
        const arquivo = input.files[0];
        mostrarAviso('');
        if (!arquivo) {
            titulo.textContent = TEXTO_PADRAO;
            area.classList.remove('com-arquivo');
            return;
        }
        titulo.textContent = arquivo.name + ' · ' + tamanhoLegivel(arquivo.size);
        area.classList.add('com-arquivo');
        if (arquivo.size > LIMITE_BYTES) {
            mostrarAviso('Este arquivo passa de 10MB. Escolha um arquivo menor.');
        }
    }

    function restaurarBotao() {
        botao.disabled = false;
        botao.classList.remove('processando');
        botaoTexto.textContent = 'Processar documento';
    }

    input.addEventListener('change', atualizarArquivo);

    // Destaque visual enquanto um arquivo é arrastado por cima da área
    ['dragenter', 'dragover'].forEach(function (evento) {
        input.addEventListener(evento, function () { area.classList.add('arrastando'); });
    });
    ['dragleave', 'drop'].forEach(function (evento) {
        input.addEventListener(evento, function () { area.classList.remove('arrastando'); });
    });

    form.addEventListener('submit', function (evento) {
        const arquivo = input.files[0];
        if (arquivo && arquivo.size > LIMITE_BYTES) {
            evento.preventDefault(); // nem envia: economiza o upload de um arquivo que seria recusado
            mostrarAviso('Este arquivo passa de 10MB. Escolha um arquivo menor.');
            return;
        }
        botao.disabled = true;
        botao.classList.add('processando');
        botaoTexto.textContent = 'Processando… isso leva alguns segundos';
    });

    // Ao voltar para a página pelo botão "voltar" do navegador, o botão não pode ficar travado
    window.addEventListener('pageshow', function () {
        restaurarBotao();
        atualizarArquivo();
    });
})();
