import os
from flask import Flask, render_template, request, jsonify
import requests

app = Flask(__name__)

# API base URL (configurable via environment variable)
API_BASE_URL = os.getenv('API_BASE_URL', 'http://localhost:8080/api')

# API Client functions
def api_get(endpoint):
    """GET request to API"""
    try:
        response = requests.get(f"{API_BASE_URL}{endpoint}")
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f"API Error: {e}")
        return None

def api_post(endpoint, data):
    """POST request to API"""
    try:
        response = requests.post(f"{API_BASE_URL}{endpoint}", json=data)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f"API Error: {e}")
        return None

def api_put(endpoint, data):
    """PUT request to API"""
    try:
        response = requests.put(f"{API_BASE_URL}{endpoint}", json=data)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f"API Error: {e}")
        return None

def api_delete(endpoint):
    """DELETE request to API"""
    try:
        response = requests.delete(f"{API_BASE_URL}{endpoint}")
        response.raise_for_status()
        return True
    except requests.exceptions.RequestException as e:
        print(f"API Error: {e}")
        return False

# Home route
@app.route('/')
def index():
    return render_template('index.html')

# ============= MUEBLES ROUTES =============

@app.route('/muebles')
def muebles_list():
    muebles = api_get('/muebles') or []
    return render_template('muebles/list.html', muebles=muebles)

@app.route('/muebles/new')
def muebles_new():
    return render_template('muebles/form.html', mueble=None)

@app.route('/muebles/create', methods=['POST'])
def muebles_create():
    data = {
        'nombre': request.form.get('nombre'),
        'tipo': request.form.get('tipo'),
        'precioBase': int(request.form.get('precioBase')),
        'stock': int(request.form.get('stock')),
        'tamano': request.form.get('tamano'),
        'material': request.form.get('material')
    }
    result = api_post('/muebles', data)
    if result:
        # Return success message for HTMX
        return render_template('muebles/list.html',
                             muebles=api_get('/muebles') or [],
                             success="Mueble creado exitosamente")
    return "Error creando mueble", 400

@app.route('/muebles/<int:id>/edit')
def muebles_edit(id):
    mueble = api_get(f'/muebles/{id}')
    return render_template('muebles/form.html', mueble=mueble)

@app.route('/muebles/<int:id>/update', methods=['POST'])
def muebles_update(id):
    data = {
        'nombre': request.form.get('nombre'),
        'tipo': request.form.get('tipo'),
        'precioBase': int(request.form.get('precioBase')),
        'stock': int(request.form.get('stock')),
        'tamano': request.form.get('tamano'),
        'material': request.form.get('material')
    }
    result = api_put(f'/muebles/{id}', data)
    if result:
        return render_template('muebles/list.html',
                             muebles=api_get('/muebles') or [],
                             success="Mueble actualizado exitosamente")
    return "Error actualizando mueble", 400

@app.route('/muebles/<int:id>/delete', methods=['DELETE'])
def muebles_delete(id):
    if api_delete(f'/muebles/{id}'):
        return render_template('muebles/list.html',
                             muebles=api_get('/muebles') or [],
                             success="Mueble eliminado exitosamente")
    return "Error eliminando mueble", 400

# ============= VARIANTES ROUTES =============

@app.route('/variantes')
def variantes_list():
    variantes = api_get('/variantes') or []
    return render_template('variantes/list.html', variantes=variantes)

@app.route('/variantes/new')
def variantes_new():
    return render_template('variantes/form.html', variante=None)

@app.route('/variantes/create', methods=['POST'])
def variantes_create():
    data = {
        'nombre': request.form.get('nombre'),
        'costoExtra': int(request.form.get('costoExtra')),
        'tipoAplicacion': request.form.get('tipoAplicacion')
    }
    result = api_post('/variantes', data)
    if result:
        return render_template('variantes/list.html',
                             variantes=api_get('/variantes') or [],
                             success="Variante creada exitosamente")
    return "Error creando variante", 400

@app.route('/variantes/<int:id>/edit')
def variantes_edit(id):
    variante = api_get(f'/variantes/{id}')
    return render_template('variantes/form.html', variante=variante)

@app.route('/variantes/<int:id>/update', methods=['POST'])
def variantes_update(id):
    data = {
        'nombre': request.form.get('nombre'),
        'costoExtra': int(request.form.get('costoExtra')),
        'tipoAplicacion': request.form.get('tipoAplicacion')
    }
    result = api_put(f'/variantes/{id}', data)
    if result:
        return render_template('variantes/list.html',
                             variantes=api_get('/variantes') or [],
                             success="Variante actualizada exitosamente")
    return "Error actualizando variante", 400

@app.route('/variantes/<int:id>/delete', methods=['DELETE'])
def variantes_delete(id):
    if api_delete(f'/variantes/{id}'):
        return render_template('variantes/list.html',
                             variantes=api_get('/variantes') or [],
                             success="Variante eliminada exitosamente")
    return "Error eliminando variante", 400

# ============= ORDENES ROUTES =============

@app.route('/ordenes')
def ordenes_list():
    ordenes = api_get('/ordenes') or []
    return render_template('ordenes/list.html', ordenes=ordenes)

@app.route('/ordenes/new')
def ordenes_new():
    muebles = api_get('/muebles') or []
    variantes = api_get('/variantes') or []
    return render_template('ordenes/form.html', muebles=muebles, variantes=variantes)

@app.route('/ordenes/create', methods=['POST'])
def ordenes_create():
    # Parse form data for order creation
    detalles = []

    # Get number of details
    num_detalles = int(request.form.get('num_detalles', 0))

    for i in range(num_detalles):
        id_mueble = request.form.get(f'detalles[{i}][idMueble]')
        cantidad = request.form.get(f'detalles[{i}][cantidad]')
        variantes_str = request.form.get(f'detalles[{i}][variantes]', '')

        if id_mueble and cantidad:
            variantes_ids = [int(v) for v in variantes_str.split(',') if v]
            detalles.append({
                'idMueble': int(id_mueble),
                'cantidad': int(cantidad),
                'idsVariantes': variantes_ids
            })

    if detalles:
        data = {'detalles': detalles}
        result = api_post('/ordenes', data)
        if result:
            return render_template('ordenes/list.html',
                                 ordenes=api_get('/ordenes') or [],
                                 success="Orden creada exitosamente")

    return "Error creando orden", 400

@app.route('/ordenes/<int:id>')
def ordenes_detail(id):
    orden = api_get(f'/ordenes/{id}')
    muebles = api_get('/muebles') or []
    variantes = api_get('/variantes') or []
    return render_template('ordenes/detail.html', orden=orden, muebles=muebles, variantes=variantes)

@app.route('/ordenes/<int:id>/confirmar', methods=['POST'])
def ordenes_confirmar(id):
    result = api_post(f'/ordenes/{id}/confirmar', {})
    if result:
        return render_template('ordenes/detail.html',
                             orden=result,
                             muebles=api_get('/muebles') or [],
                             variantes=api_get('/variantes') or [],
                             success="Orden confirmada exitosamente (Estado: VENTA, precios congelados)")
    return "Error confirmando orden", 400

@app.route('/ordenes/<int:id>/cancelar', methods=['POST'])
def ordenes_cancelar(id):
    result = api_post(f'/ordenes/{id}/cancelar', {})
    if result:
        return render_template('ordenes/detail.html',
                             orden=result,
                             muebles=api_get('/muebles') or [],
                             variantes=api_get('/variantes') or [],
                             success="Orden cancelada exitosamente")
    return "Error cancelando orden", 400

@app.route('/ordenes/<int:orden_id>/detalles/<int:detalle_id>/delete', methods=['DELETE'])
def ordenes_detalle_delete(orden_id, detalle_id):
    if api_delete(f'/ordenes/{orden_id}/detalles/{detalle_id}'):
        orden = api_get(f'/ordenes/{orden_id}')
        return render_template('ordenes/detail.html',
                             orden=orden,
                             muebles=api_get('/muebles') or [],
                             variantes=api_get('/variantes') or [],
                             success="Detalle eliminado exitosamente")
    return "Error eliminando detalle (solo permitido en COTIZACION)", 400

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
