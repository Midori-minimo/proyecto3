/* app.js - PeruTec frontend logic */
const USUARIO_LOGUEADO = (typeof window !== 'undefined' && window.USUARIO_LOGUEADO_GLOBAL) || false;
let productoSeleccionado = null;
let carrito = [];
const SS_KEY_CARRITO = 'carritoPeruTec';

window.abrirModalCompra = function(boton) {
    if (!USUARIO_LOGUEADO) {
        const modalCuenta = bootstrap.Modal.getOrCreateInstance(document.getElementById('modalCuenta'));
        modalCuenta.show();
        return;
    }
    const id = boton.getAttribute('data-id');
    const nombre = boton.getAttribute('data-nombre');
    const precio = parseFloat(boton.getAttribute('data-precio'));
    productoSeleccionado = { id, nombre, precio };
    document.getElementById('inputProductoId').value = id;
    document.getElementById('nombreProductoModal').textContent = nombre;
    document.getElementById('precioProductoModal').textContent = 'S/ ' + precio.toFixed(2);
    const modal = bootstrap.Modal.getOrCreateInstance(document.getElementById('modalCompra'));
    modal.show();
};

window.buscarProductos = function(texto) {
    texto = (texto || '').toLowerCase().trim();
    document.querySelectorAll('.producto-card').forEach(function(card) {
        const nombre = (card.querySelector('.producto-nombre')?.textContent || '').toLowerCase();
        card.style.display = (texto === '' || nombre.indexOf(texto) !== -1) ? '' : 'none';
    });
};

window.toggleFiltros = function() {
    const panel = document.querySelector('.panel-filtros');
    if (panel) panel.classList.toggle('collapsed');
};

window.extraerMarca = function(nombre) {
    if (!nombre) return 'Otro';
    const p = nombre.trim().split(/\s+/)[0].toLowerCase();
    if (p === 'iphone') return 'iPhone';
    return p.charAt(0).toUpperCase() + p.slice(1);
};

window.aplicarFiltros = function() {
    const marcasSel = Array.from(document.querySelectorAll('#listaMarcasFiltro input:checked')).map(cb => cb.value.toLowerCase());
    const minRaw = document.getElementById('precioMin')?.value || '';
    const maxRaw = document.getElementById('precioMax')?.value || '';
    const min = minRaw === '' ? 0 : parseFloat(minRaw);
    const max = maxRaw === '' ? Infinity : parseFloat(maxRaw);
    document.querySelectorAll('.producto-card').forEach(function(card) {
        const nombre = card.querySelector('.producto-nombre')?.textContent || '';
        const precioText = card.querySelector('.producto-precio')?.textContent || '0';
        const precio = parseFloat(precioText.replace(/[^\d.]/g, '')) || 0;
        const marca = extraerMarca(nombre).toLowerCase();
        const cumpleMarca = marcasSel.length === 0 || marcasSel.indexOf(marca) !== -1;
        const cumplePrecio = precio >= min && precio <= max;
        card.style.display = (cumpleMarca && cumplePrecio) ? '' : 'none';
    });
    document.querySelectorAll('.marca-bloque').forEach(function(bloque) {
        const visiblesBloque = bloque.querySelectorAll('.producto-card:not([style*="display: none"])').length;
        bloque.style.display = visiblesBloque === 0 ? 'none' : '';
    });
};

window.limpiarFiltros = function() {
    document.querySelectorAll('#listaMarcasFiltro input').forEach(cb => cb.checked = false);
    if (document.getElementById('precioMin')) document.getElementById('precioMin').value = '';
    if (document.getElementById('precioMax')) document.getElementById('precioMax').value = '';
    aplicarFiltros();
};

function cargarCarrito() {
    try {
        const data = sessionStorage.getItem(SS_KEY_CARRITO);
        carrito = data ? JSON.parse(data) : [];
    } catch (e) { carrito = []; }
    actualizarBadgeCarrito();
}

function guardarCarrito() {
    try { sessionStorage.setItem(SS_KEY_CARRITO, JSON.stringify(carrito)); } catch (e) {}
    actualizarBadgeCarrito();
}

function actualizarBadgeCarrito() {
    const badge = document.getElementById('badgeCarrito');
    if (!badge) return;
    const total = carrito.reduce((s, i) => s + (i.cantidad || 1), 0);
    badge.textContent = total;
    badge.classList.toggle('vacio', total === 0);
}

function agregarAlCarrito(p) {
    const ex = carrito.find(i => i.id === p.id);
    if (ex) { ex.cantidad = (ex.cantidad || 1) + 1; }
    else { carrito.push({ id: p.id, nombre: p.nombre, precio: parseFloat(p.precio), cantidad: 1 }); }
    guardarCarrito();
}

window.eliminarDelCarrito = function(id) {
    carrito = carrito.filter(i => i.id !== id);
    guardarCarrito(); renderCarrito();
};

window.vaciarCarrito = function() {
    if (!carrito.length) return;
    if (!confirm('¿Vaciar todo el carrito?')) return;
    carrito = []; guardarCarrito(); renderCarrito();
};

function escapeHtml(t) { const d = document.createElement('div'); d.textContent = t; return d.innerHTML; }

function renderCarrito() {
    const body = document.getElementById('carritoBody');
    const footer = document.getElementById('carritoFooter');
    const boleta = document.getElementById('boletaCarrito');
    const boletaItems = document.getElementById('boletaItems');
    if (!body) return;

    if (!carrito.length) {
        body.innerHTML = '<div class="text-center text-muted py-4"><i class="bi bi-cart-x d-block mb-2" style="font-size:3rem;"></i><p class="mb-0">Tu carrito está vacío</p></div>';
        footer.style.display = 'none';
        boleta.style.display = 'none';
        return;
    }

    let html = '';
    carrito.forEach((item, idx) => {
        const sub = item.precio * (item.cantidad || 1);
        html += '<div class="d-flex gap-2 align-items-center py-2 border-bottom">' +
            '<div class="flex-grow-1">' +
            '<div class="fw-semibold small">' + escapeHtml(item.nombre) + '</div>' +
            '<div class="text-primary small">S/ ' + item.precio.toFixed(2) + (item.cantidad > 1 ? ' × ' + item.cantidad + ' = S/ ' + sub.toFixed(2) : '') + '</div>' +
            '</div>' +
            '<button class="btn btn-sm btn-link text-danger p-0" onclick="eliminarDelCarrito(\'' + item.id + '\')" title="Eliminar">' +
            '<i class="bi bi-trash3"></i></button>' +
            '</div>';
    });
    body.innerHTML = html;
    footer.style.display = 'flex';

    let total = 0;
    let boletaHtml = '';
    carrito.forEach((item, idx) => {
        const sub = item.precio * (item.cantidad || 1);
        total += sub;
        boletaHtml += '<tr>' +
            '<td>' + (idx + 1) + '</td>' +
            '<td>' + escapeHtml(item.nombre) + '</td>' +
            '<td class="text-end">S/ ' + item.precio.toFixed(2) + '</td>' +
            '<td class="text-center">' + item.cantidad + '</td>' +
            '<td class="text-end">S/ ' + sub.toFixed(2) + '</td>' +
            '</tr>';
    });
    boletaItems.innerHTML = boletaHtml;

    const subtotal = total / 1.18;
    const igv = total - subtotal;
    document.getElementById('boletaSubtotal').textContent = 'S/ ' + subtotal.toFixed(2);
    document.getElementById('boletaIgv').textContent = 'S/ ' + igv.toFixed(2);
    document.getElementById('boletaTotal').textContent = 'S/ ' + total.toFixed(2);
    boleta.style.display = 'block';
}

window.finalizarCompraCarrito = function() {
    if (!carrito.length) return;
    if (!confirm('¿Confirmar la compra de ' + carrito.length + ' producto(s)?')) return;

    const { jsPDF } = window.jspdf;
    const doc = new jsPDF({ unit: 'mm', format: 'a4' });
    const W = 210;
    const fechaStr = new Date().toLocaleDateString('es-PE');

    /* cabecera */
    doc.setFillColor(255, 107, 53);
    doc.rect(0, 0, W, 38, 'F');
    doc.setTextColor(255, 255, 255);
    doc.setFontSize(18);
    doc.setFont('helvetica', 'bold');
    doc.text('PeruTec', 14, 16);
    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.text('RUC: 20123456789  |  Lima, Peru', 14, 24);
    doc.text('BOLETA DE VENTA ELECTRONICA', 14, 30);

    const nBoleta = 'B001-' + String(Math.floor(Math.random() * 99999)).padStart(5, '0');
    doc.setFontSize(9);
    doc.text('N° ' + nBoleta, W - 14, 24, { align: 'right' });
    doc.text('Fecha: ' + fechaStr, W - 14, 30, { align: 'right' });

    /* cliente */
    doc.setTextColor(40, 40, 40);
    let y = 48;
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(10);
    doc.text('Cliente: Consumidor Final', 14, y);
    y += 8;

    /* tabla productos */
    doc.setFontSize(9);
    doc.setFont('helvetica', 'bold');
    doc.setFillColor(243, 244, 246);
    doc.rect(14, y, W - 28, 8, 'F');
    doc.text('#', 17, y + 5.5);
    doc.text('Producto', 25, y + 5.5);
    doc.text('P.Unit', 130, y + 5.5, { align: 'right' });
    doc.text('Cant.', 155, y + 5.5, { align: 'right' });
    doc.text('Subtotal', W - 17, y + 5.5, { align: 'right' });
    y += 8;

    doc.setFont('helvetica', 'normal');
    let total = 0;
    carrito.forEach((item, idx) => {
        const sub = item.precio * (item.cantidad || 1);
        total += sub;
        doc.text(String(idx + 1), 17, y + 5.5);
        const nombreCorto = item.nombre.length > 40 ? item.nombre.substring(0, 40) + '...' : item.nombre;
        doc.text(nombreCorto, 25, y + 5.5);
        doc.text('S/ ' + item.precio.toFixed(2), 130, y + 5.5, { align: 'right' });
        doc.text(String(item.cantidad), 155, y + 5.5, { align: 'right' });
        doc.text('S/ ' + sub.toFixed(2), W - 17, y + 5.5, { align: 'right' });
        doc.setDrawColor(229, 231, 235);
        doc.line(14, y + 8, W - 14, y + 8);
        y += 8;
        if (y > 250) {
            doc.addPage();
            y = 20;
        }
    });

    /* totales */
    y += 5;
    const subtotal = total / 1.18;
    const igv = total - subtotal;
    doc.setFillColor(249, 250, 251);
    doc.roundedRect(W - 80, y, 66, 28, 2, 2, 'F');
    doc.setFont('helvetica', 'normal');
    doc.text('Subtotal (sin IGV):', W - 76, y + 8);
    doc.text('S/ ' + subtotal.toFixed(2), W - 18, y + 8, { align: 'right' });
    doc.text('IGV (18%):', W - 76, y + 14);
    doc.text('S/ ' + igv.toFixed(2), W - 18, y + 14, { align: 'right' });
    doc.setDrawColor(180, 180, 180);
    doc.line(W - 76, y + 17, W - 18, y + 17);
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(11);
    doc.setTextColor(255, 107, 53);
    doc.text('TOTAL:', W - 76, y + 24);
    doc.text('S/ ' + total.toFixed(2), W - 18, y + 24, { align: 'right' });

    /* pie */
    doc.setFontSize(8);
    doc.setTextColor(150, 150, 150);
    doc.setFont('helvetica', 'normal');
    doc.text('Gracias por su compra en PeruTec  -  Valido como comprobante de pago', W / 2, y + 40, { align: 'center' });

    doc.save('boleta_perutec_' + nBoleta + '.pdf');

    /* vaciar carrito y notificar */
    carrito = [];
    guardarCarrito();
    renderCarrito();
    bootstrap.Modal.getInstance(document.getElementById('modalCarrito'))?.hide();
    alert('¡Compra realizada! Se descargó la boleta en PDF.');
};

document.querySelector('#modalCompra form')?.addEventListener('submit', function() {
    if (productoSeleccionado) agregarAlCarrito(productoSeleccionado);
});

document.getElementById('modalCarrito')?.addEventListener('show.bs.modal', renderCarrito);

(function init() {
    cargarCarrito();
    const params = new URLSearchParams(window.location.search);
    if (params.get('compra') === 'confirmada') {
        setTimeout(() => alert('¡Compra confirmada con éxito!'), 500);
        window.history.replaceState({}, document.title, window.location.pathname);
    }
})();
