import os
from flask import Flask, render_template, request, jsonify, session, redirect, url_for
from functools import wraps
import requests

app = Flask(__name__)
app.secret_key = os.getenv('SECRET_KEY', 'dev-secret-key-change-in-production')

# API base URL (configurable via environment variable)
API_BASE_URL = os.getenv('API_BASE_URL', 'http://localhost:8080/api')

# Make user info available to all templates
@app.context_processor
def inject_user():
    return dict(
        current_user=get_current_user(),
        is_authenticated=is_authenticated(),
        is_admin=is_admin()
    )

# ============= AUTHENTICATION HELPERS =============

def get_auth_headers():
    """Get authorization headers with JWT token from session"""
    token = session.get('jwt_token')
    if token:
        return {'Authorization': f'Bearer {token}'}
    return {}

def get_current_user():
    """Get current user info from session"""
    return {
        'username': session.get('username'),
        'rol': session.get('rol'),
        'token': session.get('jwt_token')
    }

def is_authenticated():
    """Check if user is logged in"""
    return 'jwt_token' in session

def is_admin():
    """Check if current user is admin"""
    return session.get('rol') == 'ADMIN'

def login_required(f):
    """Decorator to require authentication"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if not is_authenticated():
            return redirect(url_for('login', next=request.url))
        return f(*args, **kwargs)
    return decorated_function

def admin_required(f):
    """Decorator to require admin role"""
    @wraps(f)
    def decorated_function(*args, **kwargs):
        if not is_authenticated():
            return redirect(url_for('login', next=request.url))
        if not is_admin():
            return "Acceso denegado: Se requiere rol de administrador", 403
        return f(*args, **kwargs)
    return decorated_function

# API Client functions
def api_get(endpoint, authenticated=True):
    """GET request to API"""
    try:
        headers = get_auth_headers() if authenticated else {}
        response = requests.get(f"{API_BASE_URL}{endpoint}", headers=headers)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f"API Error: {e}")
        return None

def api_post(endpoint, data, authenticated=True):
    """POST request to API"""
    try:
        headers = get_auth_headers() if authenticated else {}
        headers['Content-Type'] = 'application/json'
        response = requests.post(f"{API_BASE_URL}{endpoint}", json=data, headers=headers)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f"API Error: {e}")
        if hasattr(e.response, 'text'):
            print(f"Response text: {e.response.text}")
        return None

def api_put(endpoint, data, authenticated=True):
    """PUT request to API"""
    try:
        headers = get_auth_headers() if authenticated else {}
        response = requests.put(f"{API_BASE_URL}{endpoint}", json=data, headers=headers)
        response.raise_for_status()
        return response.json()
    except requests.exceptions.RequestException as e:
        print(f"API Error: {e}")
        return None

def api_delete(endpoint, authenticated=True):
    """DELETE request to API"""
    try:
        headers = get_auth_headers() if authenticated else {}
        response = requests.delete(f"{API_BASE_URL}{endpoint}", headers=headers)
        response.raise_for_status()
        return True
    except requests.exceptions.RequestException as e:
        print(f"API Error: {e}")
        return False

# ============= AUTHENTICATION ROUTES =============

@app.route('/login', methods=['GET', 'POST'])
def login():
    if request.method == 'GET':
        return render_template('auth/login.html')

    # POST: Process login
    username = request.form.get('username')
    password = request.form.get('password')

    # Call API
    response = api_post('/auth/login', {'username': username, 'password': password}, authenticated=False)

    if response and 'token' in response:
        # Save to session
        session['jwt_token'] = response['token']
        session['username'] = response['username']
        session['rol'] = response['rol']

        # Redirect based on role
        if response['rol'] == 'ADMIN':
            return redirect(url_for('admin_dashboard'))
        else:
            return redirect(url_for('catalogo'))
    else:
        return render_template('auth/login.html', error="Credenciales inválidas")

@app.route('/register', methods=['GET', 'POST'])
def register():
    if request.method == 'GET':
        return render_template('auth/register.html')

    # POST: Process registration
    username = request.form.get('username')
    email = request.form.get('email')
    password = request.form.get('password')
    password_confirm = request.form.get('password_confirm')

    # Validate passwords match
    if password != password_confirm:
        return render_template('auth/register.html', error="Las contraseñas no coinciden")

    # Call API
    response = api_post('/auth/register', {
        'username': username,
        'email': email,
        'password': password
    }, authenticated=False)

    if response and 'message' in response:
        return render_template('auth/register.html', success="Registro exitoso. Ya puedes iniciar sesión.")
    else:
        error_msg = response.get('error', 'Error en el registro') if response else 'Error en el registro'
        return render_template('auth/register.html', error=error_msg)

@app.route('/logout')
def logout():
    session.clear()
    return redirect(url_for('login'))

# ============= HOME & CATALOG ROUTES =============

@app.route('/')
def index():
    if not is_authenticated():
        return redirect(url_for('login'))

    if is_admin():
        return redirect(url_for('admin_dashboard'))
    else:
        return redirect(url_for('catalogo'))

@app.route('/catalogo')
@login_required
def catalogo():
    muebles = api_get('/muebles', authenticated=False) or []  # Catalog is public
    return render_template('catalogo.html', muebles=muebles)

# ============= ADMIN ROUTES =============

@app.route('/admin')
@admin_required
def admin_dashboard():
    return render_template('admin/dashboard.html')

# ============= ADMIN MUEBLES ROUTES =============

@app.route('/admin/muebles')
@admin_required
def muebles_list():
    muebles = api_get('/muebles', authenticated=False) or []
    return render_template('muebles/list.html', muebles=muebles)

@app.route('/admin/muebles/new')
@admin_required
def muebles_new():
    return render_template('muebles/form.html', mueble=None)

@app.route('/admin/muebles/create', methods=['POST'])
@admin_required
def muebles_create():
    data = {
        'nombre': request.form.get('nombre'),
        'tipo': request.form.get('tipo'),
        'precioBase': int(request.form.get('precioBase')),
        'stock': int(request.form.get('stock')),
        'tamano': request.form.get('tamano'),
        'material': request.form.get('material')
    }
    result = api_post('/muebles', data, authenticated=True)
    if result:
        # Return partial template for HTMX (without base.html)
        return render_template('muebles/list_partial.html',
                             muebles=api_get('/muebles', authenticated=False) or [],
                             success="Mueble creado exitosamente")
    return "Error creando mueble", 400

@app.route('/admin/muebles/<int:id>/edit')
@admin_required
def muebles_edit(id):
    mueble = api_get(f'/muebles/{id}')
    return render_template('muebles/form.html', mueble=mueble)

@app.route('/admin/muebles/<int:id>/update', methods=['POST'])
@admin_required
def muebles_update(id):
    data = {
        'nombre': request.form.get('nombre'),
        'tipo': request.form.get('tipo'),
        'precioBase': int(request.form.get('precioBase')),
        'stock': int(request.form.get('stock')),
        'tamano': request.form.get('tamano'),
        'material': request.form.get('material')
    }
    result = api_put(f'/muebles/{id}', data, authenticated=True)
    if result:
        return render_template('muebles/list_partial.html',
                             muebles=api_get('/muebles', authenticated=False) or [],
                             success="Mueble actualizado exitosamente")
    return "Error actualizando mueble", 400

@app.route('/admin/muebles/<int:id>/delete', methods=['DELETE'])
@admin_required
def muebles_delete(id):
    if api_delete(f'/muebles/{id}', authenticated=True):
        return render_template('muebles/list_partial.html',
                             muebles=api_get('/muebles', authenticated=False) or [],
                             success="Mueble eliminado exitosamente")
    return "Error eliminando mueble", 400

# ============= ADMIN VARIANTES ROUTES =============

@app.route('/admin/variantes')
@admin_required
def variantes_list():
    variantes = api_get('/variantes', authenticated=False) or []
    return render_template('variantes/list.html', variantes=variantes)

@app.route('/admin/variantes/new')
@admin_required
def variantes_new():
    return render_template('variantes/form.html', variante=None)

@app.route('/admin/variantes/create', methods=['POST'])
@admin_required
def variantes_create():
    data = {
        'nombre': request.form.get('nombre'),
        'costoExtra': int(request.form.get('costoExtra')),
        'tipoAplicacion': request.form.get('tipoAplicacion')
    }
    result = api_post('/variantes', data, authenticated=True)
    if result:
        return render_template('variantes/list_partial.html',
                             variantes=api_get('/variantes', authenticated=False) or [],
                             success="Variante creada exitosamente")
    return "Error creando variante", 400

@app.route('/admin/variantes/<int:id>/edit')
@admin_required
def variantes_edit(id):
    variante = api_get(f'/variantes/{id}', authenticated=False)
    return render_template('variantes/form.html', variante=variante)

@app.route('/admin/variantes/<int:id>/update', methods=['POST'])
@admin_required
def variantes_update(id):
    data = {
        'nombre': request.form.get('nombre'),
        'costoExtra': int(request.form.get('costoExtra')),
        'tipoAplicacion': request.form.get('tipoAplicacion')
    }
    result = api_put(f'/variantes/{id}', data, authenticated=True)
    if result:
        return render_template('variantes/list_partial.html',
                             variantes=api_get('/variantes', authenticated=False) or [],
                             success="Variante actualizada exitosamente")
    return "Error actualizando variante", 400

@app.route('/admin/variantes/<int:id>/delete', methods=['DELETE'])
@admin_required
def variantes_delete(id):
    if api_delete(f'/variantes/{id}', authenticated=True):
        return render_template('variantes/list_partial.html',
                             variantes=api_get('/variantes', authenticated=False) or [],
                             success="Variante eliminada exitosamente")
    return "Error eliminando variante", 400

# ============= USER ORDENES ROUTES (Mis Cotizaciones) =============

@app.route('/mis-ordenes')
@login_required
def mis_ordenes_list():
    ordenes = api_get('/ordenes', authenticated=True) or []
    return render_template('ordenes/list.html', ordenes=ordenes)

@app.route('/mis-ordenes/new')
@login_required
def mis_ordenes_new():
    muebles = api_get('/muebles', authenticated=False) or []
    variantes = api_get('/variantes', authenticated=False) or []
    return render_template('ordenes/form.html', muebles=muebles, variantes=variantes)

# ============= ADMIN ORDENES ROUTES =============

@app.route('/admin/ordenes')
@admin_required
def ordenes_list():
    ordenes = api_get('/ordenes', authenticated=True) or []
    return render_template('ordenes/list.html', ordenes=ordenes)

@app.route('/admin/ordenes/new')
@admin_required
def admin_ordenes_new():
    muebles = api_get('/muebles', authenticated=False) or []
    variantes = api_get('/variantes', authenticated=False) or []
    return render_template('ordenes/form.html', muebles=muebles, variantes=variantes, is_admin=True)

@app.route('/admin/ordenes/create', methods=['POST'])
@admin_required
def admin_ordenes_create():
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

    if not detalles:
        return render_template('ordenes/list.html',
                             ordenes=api_get('/ordenes', authenticated=True) or [],
                             error="Debe agregar al menos un item a la orden")

    data = {'detalles': detalles}
    print(f"Admin sending order data: {data}")
    result = api_post('/ordenes', data, authenticated=True)
    if result:
        return redirect(url_for('ordenes_list'))

    return render_template('ordenes/list.html',
                         ordenes=api_get('/ordenes', authenticated=True) or [],
                         error="Error al crear la orden. Por favor revise los logs.")

@app.route('/admin/ordenes/<int:id>')
@admin_required
def admin_ordenes_detail(id):
    orden = api_get(f'/ordenes/{id}', authenticated=True)
    muebles = api_get('/muebles', authenticated=False) or []
    variantes = api_get('/variantes', authenticated=False) or []
    return render_template('ordenes/detail.html', orden=orden, muebles=muebles, variantes=variantes, is_admin=True)

# ============= ADMIN USUARIOS ROUTES =============

@app.route('/admin/usuarios')
@admin_required
def usuarios_list():
    usuarios = api_get('/usuarios', authenticated=True) or []
    return render_template('admin/usuarios.html', usuarios=usuarios)

@app.route('/admin/usuarios/<int:id>/promover', methods=['PUT'])
@admin_required
def usuarios_promover(id):
    result = api_put(f'/usuarios/{id}/rol', {'rol': 'ADMIN'}, authenticated=True)
    if result:
        return render_template('admin/usuarios_partial.html',
                             usuarios=api_get('/usuarios', authenticated=True) or [],
                             success=f"Usuario promovido a ADMIN exitosamente")
    return "Error promoviendo usuario", 400

@app.route('/admin/usuarios/<int:id>/degradar', methods=['PUT'])
@admin_required
def usuarios_degradar(id):
    result = api_put(f'/usuarios/{id}/rol', {'rol': 'USER'}, authenticated=True)
    if result:
        return render_template('admin/usuarios_partial.html',
                             usuarios=api_get('/usuarios', authenticated=True) or [],
                             success=f"Usuario degradado a USER exitosamente")
    return "Error degradando usuario", 400

@app.route('/admin/usuarios/<int:id>/toggle-activo', methods=['PUT'])
@admin_required
def usuarios_toggle_activo(id):
    result = api_put(f'/usuarios/{id}/activar', {}, authenticated=True)
    if result:
        estado = "activado" if result.get('activo') else "desactivado"
        return render_template('admin/usuarios_partial.html',
                             usuarios=api_get('/usuarios', authenticated=True) or [],
                             success=f"Usuario {estado} exitosamente")
    return "Error cambiando estado de usuario", 400

@app.route('/mis-ordenes/create', methods=['POST'])
@login_required
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

    if not detalles:
        return render_template('ordenes/list.html',
                             ordenes=api_get('/ordenes', authenticated=True) or [],
                             error="Debe agregar al menos un item a la orden")

    data = {'detalles': detalles}
    print(f"Sending order data: {data}")
    result = api_post('/ordenes', data, authenticated=True)
    if result:
        return redirect(url_for('mis_ordenes_list'))

    return render_template('ordenes/list.html',
                         ordenes=api_get('/ordenes', authenticated=True) or [],
                         error="Error al crear la orden. Por favor revise los logs.")

@app.route('/mis-ordenes/<int:id>')
@login_required
def mis_ordenes_detail(id):
    orden = api_get(f'/ordenes/{id}', authenticated=True)
    muebles = api_get('/muebles', authenticated=False) or []
    variantes = api_get('/variantes', authenticated=False) or []
    return render_template('ordenes/detail.html', orden=orden, muebles=muebles, variantes=variantes)

@app.route('/mis-ordenes/<int:id>/confirmar', methods=['POST'])
@login_required
def mis_ordenes_confirmar(id):
    result = api_post(f'/ordenes/{id}/confirmar', {}, authenticated=True)
    if result:
        return render_template('ordenes/detail_partial.html',
                             orden=result,
                             muebles=api_get('/muebles', authenticated=False) or [],
                             variantes=api_get('/variantes', authenticated=False) or [],
                             success="Orden confirmada exitosamente (Estado: VENTA, precios congelados)")
    return "Error confirmando orden", 400

@app.route('/mis-ordenes/<int:id>/cancelar', methods=['POST'])
@login_required
def mis_ordenes_cancelar(id):
    result = api_post(f'/ordenes/{id}/cancelar', {}, authenticated=True)
    if result:
        return render_template('ordenes/detail_partial.html',
                             orden=result,
                             muebles=api_get('/muebles', authenticated=False) or [],
                             variantes=api_get('/variantes', authenticated=False) or [],
                             success="Orden cancelada exitosamente")
    return "Error cancelando orden", 400

@app.route('/mis-ordenes/<int:orden_id>/detalles/<int:detalle_id>/delete', methods=['DELETE'])
@login_required
def mis_ordenes_detalle_delete(orden_id, detalle_id):
    if api_delete(f'/ordenes/{orden_id}/detalles/{detalle_id}', authenticated=True):
        orden = api_get(f'/ordenes/{orden_id}', authenticated=True)
        return render_template('ordenes/detail_partial.html',
                             orden=orden,
                             muebles=api_get('/muebles', authenticated=False) or [],
                             variantes=api_get('/variantes', authenticated=False) or [],
                             success="Detalle eliminado exitosamente")
    return "Error eliminando detalle (solo permitido en COTIZACION)", 400

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
