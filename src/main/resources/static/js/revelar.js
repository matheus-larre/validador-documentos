// Faz cada bloco com a classe "revelar" surgir quando entra na tela (ao rolar a página).
// IntersectionObserver avisa quando o elemento fica visível, sem precisar ouvir o evento de rolagem.
(function () {
    const elementos = document.querySelectorAll('.revelar');
    const reduzirAnimacao = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    // Navegador antigo ou usuário que prefere menos animação: mostra tudo de uma vez
    if (!('IntersectionObserver' in window) || reduzirAnimacao) {
        elementos.forEach(function (el) { el.classList.add('visivel'); });
        return;
    }

    const observador = new IntersectionObserver(function (entradas) {
        entradas.forEach(function (entrada) {
            if (entrada.isIntersecting) {
                entrada.target.classList.add('visivel');
                observador.unobserve(entrada.target); // surge uma vez só
            }
        });
    }, { threshold: 0.15 });

    elementos.forEach(function (el) { observador.observe(el); });
})();
