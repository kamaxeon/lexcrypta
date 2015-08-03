# LEXCrypta
## ¿Qué es?

LEXCrypta es una plataforma para la compartición confidencial de ficheros de manera fácil y segura.

LEXCrypta se diseñó pensando en unos objetivos básicos:

* Cualquiera podrá subir un fichero a LEXCrypta (no será necesario registro previo)
* Sólo alguien con las claves necesarias podrá descargar un fichero subido
* Toda la información se guardará cifrada en el servidor, de tal forma que ni siquiera alguien con acceso directo al servidor (administrador o hacker) pueda descifrar el contenido de los ficheros (por lo tanto las claves no se guardarán en el servidor)

## ¿Por qué?
La idea surgió de un amigo abogado (de ahí lo de "LEX") que me hacía comentarios del tipo: *"tal cliente me envío unos informes"*, o *"los procuradores envían a los abogados los documentos que..."*, o *"aquel perito me envío su peritaje..."*, o *"tengo que enviar a mi colega los papeles del caso..."*.

Todo esto no sería preocupante si no fuera porque estas transmisiones de documentos con un grado bastante alto de confidencialidad se hacen, la mayoría de la veces, por correo electrónico. No vamos a entrar en detalles técnicos pero el correo electrónico no es un medio seguro/confidencial de transmisión ya que no garantiza que la comunicación entre todos los nodos de la comunicación (clientes, nodos MTA, destinatarios) utilice canales cifrados. 

Por esto surgió la idea de LexNet, para crear una forma segura de transmitir estos documentos y que no fuese muy complicada de usar (los usuarios de LexNet sabrán a lo que me refiero). Y desde el principio también supe que el proyecto tenía que ser open source para garantizar la visibilidad de los algoritmos usados y permitir su revisión, correción y mejora por parte de la comunidad.

Si bien en su origen está pensado para abogados, espero que otros muchos gremios/comunidades encuentren la utilidad a LEXCrypta.

## ¿Cómo funciona?
Un usuario que quiere compartir un fichero con alguien se dirige a la pantalla de subida d e LEXCrypta y proporciona un ID de comunicación (mínimo 6 carácteres) y el fichero a subir.

El ID es una palabra/contrañesa que identifica a esta transmisión y debe ser conocida tanto por el emisor como por el receptor, ya sea porque lo han establecido de antemano o porque el emisor se la comunicará posteriormente al receptor.

Una vez subido el fichero LEXCrypta proporciona al emisor una URL de descarga con la clave criptográfica que debe enviar al receptor, ya sea por e-mail, Whatsapp, Hangouts, QR-Code, etc. 

Cuando un usuario reciba la URL de descarga y acceda a ella, se le pedirá el ID de la transmisión que sólo él y el emisor deben conocer. Una vez introducidos los datos la descarga del fichero puede comenzar.

Se puede utilizar el e-mail o cualquier medio no cifrado para enviar la URL de descarga con total tranquilidad ya que aún siendo descubierta o robada por un tercero, si éste no posee el ID de la comunicación no podrá descargar el contenido.

Es importante recordar que el ID de la comunicación _**NUNCA**_ debe transmitirse _**POR EL MISMO CANAL**_ que la URL con la clave anterior ya que  en caso de que alguien o algo (virus, troyano, etc.) tenga acceso al sistema de mensajería usado, éste podría descargar el contenido del mensaje (está en posesión de los 2 elementos necesarios para la descarga y descifrado del fichero). 

Posibles recomendacines para la transmisión del ID pueden ser: 

* transmitido por teléfono
* mensajería segura
* puede estar implícito en el propio contexto de la situación (número de expediente, NIF/CIF del sujeto, fecha del acto,...)

## Técnicamente, ¿cómo funciona?
LEXCrypta usa un protocolo muy sencillo cuando un usuario sube un fichero a la plataforma:

1. LEXCrypta solicita al usuario un ID para la transimisión. Realmente es una palabra/contraseña que se utilizará, convenientemente tratada, como semilla generadora de un cifrado AES.
2. Genera una clave AES de 128 bit (para cada fichero subido se crea una nueva clave).
3. Se trata el ID (truncándolo o ampliándolo para que tenga una longitud de 128 bit, sirviendo de Vector de Inicialización o IV en los posteriores cifrados
4. Se cifra el ID transformado del paso anterior con la clave generada en el paso 2, lo que servirá de nueva clave AES (se elimina todo lo que sobre de 128 bit, normalmente el padding empleado)
5. Con la nueva clave se cifra el ID original, utilizando el IV obtenido en el paso 3 
6. Con la nueva clave se cifra la ruta del fichero subido en el servidor (usamos también el IV del paso 3)
7. Con el mismo IV (paso 3) y la nueva clave ciframos el nombre del fichero
8. Se guarda en base de datos la tupla (ID, ruta_fichero, nombre_fichero), pero cifrado tal y como hemos explicado en 5, 6 y 7

En ningún momento se almacena ningún parámetro que pueda generar las diferentes claves utilizadas en el proceso, lo que no permite descubrir el contenido de cada tupla de base de datos, ni asociarlo a ningún fichero real en el servidor (tampoco guardamos el timestamp de la operación para dificultar relacionar registros/tuplas con ficheros mediante fecha de creación), aunque tengamos acceso a base de datos y/o sistema de archivos.

Cuando un usuario intenta bajar un fichero, con el ID proporcionado se repiten los pasos 2, 3 y 4, con lo que tenemos el ID cifrado que identifica de manera única la tupla en base de datos, con lo que obtenemos la ruta del fichero en el servidor y su nombre, y aplicando AES para descifrarlos (en 4 obtuvimos la clave final de cifrado) podemos proceder a la descarga del contenido.

Los ficheros y los registros de base de datos permanecerán una tiempo máximo predeterminado en el sistema ya que unos procesos de limpieza se ejecutarán de forma regular para garantizar que ningún archivo permanezca en el sistema más de lo deseado. En caso de que un fichero sea borrado no habrá forma de recuperarlo, por lo que si se quiere compartir otra vez habrá que realizar una nueva subida.






