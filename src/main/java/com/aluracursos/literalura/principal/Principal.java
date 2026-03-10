package com.aluracursos.literalura.principal;

import com.aluracursos.literalura.model.Autor;
import com.aluracursos.literalura.model.DatosLibro;
import com.aluracursos.literalura.model.GutendexResponse;
import com.aluracursos.literalura.model.Libro;
import com.aluracursos.literalura.repository.AutorRep;
import com.aluracursos.literalura.repository.Repository;
import com.aluracursos.literalura.service.ConsumoAPI;
import com.aluracursos.literalura.service.ConvierteDatos;
import org.jspecify.annotations.NonNull;

import java.util.Optional;
import java.util.Scanner;

public class Principal {
    private Scanner entrada = new Scanner(System.in);
    private ConsumoAPI consumoApi = new ConsumoAPI();
    private final String URL_BASE = "https://gutendex.com/books/";
    private ConvierteDatos conversor = new ConvierteDatos();
    private Repository Repository;
    private AutorRep autorRep;

    public Principal(Repository repository, AutorRep autorRep) {
        this.Repository = repository;
        this.autorRep = autorRep;
    }


    public void showMenu()
    {
        var opcion = -1;
        while (opcion != 0) {
            var menu = """
                    --------------------------------
                    Elija la opción a través de su número:
                    1 - Buscar Libro por su Título 
                    2 - Listar Libros Registrados
                    3 - Listar Autores Registrados
                    4 - Listar Autores vivos en un determinado Año
                    5 - Listar Libros por Idioma
                    0 - Salir
                    """;
            System.out.println(menu);

            opcion = entrada.nextInt();
            entrada.nextLine();

            switch (opcion) {
                case 1:
                    findLibroByTitulo();
                    break;
                case 2:
                    listarRegisteredBooks();
                    break;
                case 3:
                    listAuthorsByRegistered();
                    break;
                case 4:
                    listarAutoresVivosEnAnio();
                    break;
                case 5:
                    listLibByIdioma();
                    break;
                default:
                    if (opcion != 0) {
                        System.out.println(
                                "Opción no válida. Por favor, elija una opción del menú\n" +
                                "---------------------------\n");
                    }
                    break;
            }

        }
    }

    private void listLibByIdioma() {
        System.out.println(menuIdiomas());
        var idioma = entrada.nextLine();
        var resultado = Repository.findByIdiomaIgnoreCase(idioma);
        if (resultado.isEmpty()) {
            System.out.println("No se encontraron libros en el idioma especificado.");
        } else {
            resultado.forEach(System.out::println);
            entrada.nextLine();
        }

    }

    private void listarAutoresVivosEnAnio() {
        System.out.println("Ingrese el año vivo de autor(es) que desea buscar:");
        var anio = entrada.nextInt();
        autorRep.findAutoresVivosEnAnio(anio).forEach(System.out::println);
        entrada.nextLine();
    }

    private void listAuthorsByRegistered() {
        autorRep.findAll().forEach(System.out::println);
        entrada.nextLine();
    }

    private void listarRegisteredBooks() {
        Repository.findAll().forEach(System.out::println);
        entrada.nextLine();
    }

    private void findLibroByTitulo() {
        System.out.println("Ingrese el nombre del libro que desea buscar:");
        var titulo = entrada.nextLine();
        GutendexResponse respuesta = getLibro(titulo);

        if (respuesta.getResults() == null || respuesta.getResults().isEmpty()) {
            System.out.println("Libro no encontrado.");
            return;
        }
        DatosLibro datosLibro = respuesta.getResults().get(0);
        procesAndSave(datosLibro);
        entrada.nextLine();
    }

    private GutendexResponse getLibro(@NonNull String tittle) {
        String url = URL_BASE + "?search=" + tittle.toLowerCase().replace(" ", "%20");
        String json = consumoApi.obtenerDatos(url);
        System.out.println(json);
        return conversor.obtenerDatos(json, GutendexResponse.class);
    }
    private void procesAndSave(@NonNull DatosLibro datosLibro) {
        String nombreAutor = datosLibro.getAutor().nombre();

        if (verifyBookExist(datosLibro.titulo(), nombreAutor)) {
            return;
        }

        Autor autor = findOrCreateAuthor(nombreAutor, datosLibro);
        saveLibro(datosLibro, autor);
    }

    private boolean verifyBookExist(String titulo, String nombreAutor) {
        Optional<Libro> libroExistente = Repository
                .findByTituloAndAutor_Nombre(titulo, nombreAutor);

        if (libroExistente.isPresent()) {
            System.out.println("No se puede registrar el mismo libro mas de una vez.");
            System.out.println(libroExistente.get().toString());
            return true;
        }
        return false;
    }

    private Autor findOrCreateAuthor(String nombreAutor, DatosLibro datosLibro) {
        return autorRep.findByNombre(nombreAutor)
                .orElseGet(() -> autorRep.save(new Autor(datosLibro.getAutor())));
    }

    private void saveLibro(DatosLibro datosLibro, Autor autor) {
        Libro libro = new Libro(datosLibro, autor);
        Repository.save(libro);
        System.out.println(libro.toString());
    }
    private String menuIdiomas() {
        return """
                Ingrese el idioma para buscar los libros:\s
                en - Inglés
                es - Español
                fr - Francés
                pt - Portugués
                """;
    }



}
