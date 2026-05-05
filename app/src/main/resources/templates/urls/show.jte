@import hexlet.code.UrlPage
@import hexlet.code.UrlCheck
@param UrlPage page

@template.layout(page = page, content = @`
    <h1>Сайт: ${page.getUrl().getName()}</h1>

    <div class="table-responsive">
        <table class="table table-bordered table-hover mt-3" data-test="url">
            <tbody>
            <tr>
                <th scope="row">ID</th>
                <td>${page.getUrl().getId()}</td>
            </tr>
            <tr>
                <th scope="row">Имя</th>
                <td>${page.getUrl().getName()}</td>
            </tr>
            <tr>
                <th scope="row">Дата создания</th>
                <td>${page.getUrl().getCreatedAt()}</td>
            </tr>
            </tbody>
        </table>
    </div>

    <form action="/urls/${page.getUrl().getId()}/checks" method="post" class="mt-3">
        <button type="submit" class="btn btn-primary">Запустить проверку</button>
    </form>

    <h2 class="mt-5">Проверки</h2>
    <table class="table table-bordered table-hover mt-3">
        <thead>
        <tr>
            <th>ID</th>
            <th>Код ответа</th>
            <th>Title</th>
            <th>H1</th>
            <th>Description</th>
            <th>Дата создания</th>
        </tr>
        </thead>
        <tbody>
        @for(check : page.getChecks())
            <tr>
                <td>${check.getId()}</td>
                <td>${check.getStatusCode()}</td>
                <td>${check.getTitle() != null ? check.getTitle() : "-"}</td>
                <td>${check.getH1() != null ? check.getH1() : "-"}</td>
                <td>${check.getDescription() != null ? check.getDescription() : "-"}</td>
                <td>${check.getCreatedAt()}</td>
            </tr>
        @endfor
        </tbody>
    </table>
`)