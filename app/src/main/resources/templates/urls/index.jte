@import hexlet.code.UrlsPage
@param UrlsPage page

@template.layout(page = page, content = @`
    <h1>Сайты</h1>

    <table class="table table-bordered table-hover mt-3" data-test="urls">
        <thead>
        <tr>
            <th>ID</th>
            <th>Имя</th>
            <th>Дата создания</th>
            <th>Последняя проверка</th>
            <th>Код ответа</th>
        </tr>
        </thead>
        <tbody>
        @for(var url : page.getUrls())
            @var lastDate = page.getLastCheckDate().get(url.getId())
            @var lastCode = page.getLastCheckCode().get(url.getId())
            <tr>
                <td>${url.getId()}</td>
                <td>
                    <a href="/urls/${url.getId()}">${url.getName()}</a>
                </td>
                <td>${url.getCreatedAt() != null ? url.getCreatedAt().toString() : "-"}</td>
                <td>${lastDate != null ? lastDate.toString() : "-"}</td>
                <td>${lastCode != null ? lastCode : "-"}</td>
            </tr>
        @endfor
        </tbody>
    </table>
`)